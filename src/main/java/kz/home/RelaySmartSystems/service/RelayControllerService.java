package kz.home.RelaySmartSystems.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.entity.*;
import kz.home.RelaySmartSystems.model.dto.*;
import kz.home.RelaySmartSystems.model.mapper.*;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.*;
import kz.home.RelaySmartSystems.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.Collectors;

import kz.home.RelaySmartSystems.Utils;
import org.springframework.web.socket.BinaryMessage;

import javax.transaction.Transactional;

import static kz.home.RelaySmartSystems.model.mapper.BConfigMapper.mapAction;

@Service
public class RelayControllerService {
    private final RelayControllerRepository relayControllerRepository;
    private final RCOutputRepository outputRepository;
    private final RCInputRepository inputRepository;
    private final RelayControllerMapper relayControllerMapper;
    private final RCSchedulerMapper rcSchedulerMapper;
    private final RCSchedulerRepository rcSchedulerRepository;
    private final RCMqttMapper rcMqttMapper;
    private final RCMqttRepository rcMqttRepository;
    private final ControllerService controllerService;
    private static final Logger logger = LoggerFactory.getLogger(RelayControllerService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final DeviceConfigService deviceConfigService;

    record Patch(int pos, List<RCEvent> events) {}
    List<Patch> inputEventOffsetPatchList = new ArrayList<>();
    record PatchAction(int pos, List<RCAction> actions) {}
    List<PatchAction> eventActionOffsetPatchList = new ArrayList<>();

    public RelayControllerService(RelayControllerRepository relayControllerRepository,
                                  RCOutputRepository outputRepository,
                                  RCInputRepository inputRepository,
                                  ControllerService controllerService,
                                  RelayControllerMapper relayControllerMapper,
                                  RCSchedulerMapper rcSchedulerMapper,
                                  RCSchedulerRepository rcSchedulerRepository,
                                  RCMqttMapper rcMqttMapper,
                                  RCMqttRepository rcMqttRepository, DeviceConfigService deviceConfigService) {
        this.relayControllerRepository = relayControllerRepository;
        this.outputRepository = outputRepository;
        this.inputRepository = inputRepository;
        this.relayControllerMapper = relayControllerMapper;
        this.rcSchedulerMapper = rcSchedulerMapper;
        this.rcSchedulerRepository = rcSchedulerRepository;
        this.rcMqttMapper = rcMqttMapper;
        this.rcMqttRepository = rcMqttRepository;
        this.controllerService = controllerService;
        this.deviceConfigService = deviceConfigService;
    }

    @Transactional
    public void updateRelayControllerIOStates(RCUpdateIODTO rcUpdateIODTO) {

        // TODO : make it binary

        RelayController relayController = relayControllerRepository.findByMac(rcUpdateIODTO.getMac());
        if (relayController == null) {
            // контролер не найден
            logger.error("Relay controller with mac {} not found", rcUpdateIODTO.getMac());
            return;
        }

        for (RCUpdateIODTO.RCState out : rcUpdateIODTO.getOutputs()) {
            relayController.getOutputs().stream()
                    .filter(child -> child.getId().equals(out.getId()))
                    .findFirst()
                    .ifPresent(child -> {
                        child.setState(out.getState());
                    });
        }
        for (RCUpdateIODTO.RCState in : rcUpdateIODTO.getInputs()) {
            relayController.getInputs().stream()
                    .filter(child -> child.getId().equals(in.getId()))
                    .findFirst()
                    .ifPresent(child -> {
                        child.setState(in.getState());
                    });
        }
        relayControllerRepository.save(relayController);
    }

    public void setOutputState(String mac, Integer output, String state, Integer slaveId) {
        RelayController c = relayControllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            RCOutput o = outputRepository.findOutput(c.getUuid(), output, slaveId);
            if (o != null) {
                o.setState(state);
                outputRepository.save(o);
            }
            controllerService.updateLastSeen(c.getUuid());
        }
    }

    public void setInputState(String mac, Integer input, String state, Integer slaveId) {
        RelayController c = relayControllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            RCInput o = inputRepository.findInput(c.getUuid(), input, slaveId);
            if (o != null) {
                o.setState(state);
                inputRepository.save(o);
            }
        }
    }

    @Transactional
    public String saveConfig(RCConfigDTO rcConfigDTO) {
        // TODO : new common config
        RelayController relayController = relayControllerRepository.findByMac(rcConfigDTO.getMac());
        if (relayController != null) {
            relayController.setName(rcConfigDTO.getName());
            relayController.setDescription(rcConfigDTO.getDescription());
            //relayController.setHwParams((String)rcConfigDTO.getHwParams());
            deviceConfigService.saveConfig(rcConfigDTO.getConfig(), relayController);
        } else {
            return "NOT_FOUND";
        }
        //        saveMqttConfig(rcConfigDTO.getMqtt(), relayController);
        return "OK";
    }

    private void saveMqttConfig(RCMqttDTO rcMqttDTO,
                                RelayController relayController) {
        if (rcMqttDTO == null)
            return;
        RCMqtt rcMqtt = rcMqttMapper.toEntity(rcMqttDTO);
        rcMqtt.setController(relayController);
        rcMqttRepository.save(rcMqtt);
    }

    String saveSchedulerConfig(RCSchedulerDTO rcSchedulerDTO,
                               RelayController relayController) {
        if (rcSchedulerDTO == null || relayController == null)
            return "NULL";
        try {
            RCScheduler existingScheduler = rcSchedulerRepository.findByController(relayController);
            if (existingScheduler != null) {
                // update existing
                existingScheduler.setEnabled(rcSchedulerDTO.isEnabled());

                // --- синхронизация задач ---
                List<RCTask> existingTasks = existingScheduler.getTasks();
                List<RCSchedulerDTO.RCTaskDTO> dtoTasks = rcSchedulerDTO.getTasks() != null ? rcSchedulerDTO.getTasks() : new ArrayList<>();

                // Удаляем задачи, которых нет в DTO (по имени)
                existingTasks.removeIf(task -> dtoTasks.stream().noneMatch(dto -> dto.getName() != null && dto.getName().equals(task.getName())));

                // Обновляем существующие и добавляем новые задачи
                for (RCSchedulerDTO.RCTaskDTO dtoTask : dtoTasks) {
                    RCTask task = existingTasks.stream()
                        .filter(t -> dtoTask.getName() != null && dtoTask.getName().equals(t.getName()))
                        .findFirst()
                        .orElse(null);
                    if (task == null) {
                        // новая задача
                        task = new RCTask();
                        task.setScheduler(existingScheduler);
                        existingTasks.add(task);
                    }
                    // обновляем поля задачи
                    task.setName(dtoTask.getName());
                    task.setGrace(dtoTask.getGrace());
                    task.setTime(dtoTask.getTime());
                    task.setDone(dtoTask.isDone());
                    task.setEnabled(dtoTask.isEnabled());
                    task.setDow(dtoTask.getDow());

                    // --- синхронизация действий задачи ---
                    List<RCTaskAction> existingActions = task.getActions();
                    List<RCSchedulerDTO.RCTaskActionDTO> dtoActions = dtoTask.getActions() != null ? dtoTask.getActions() : new ArrayList<>();
                    // Удаляем действия, которых нет в DTO (по action и output)
                    existingActions.removeIf(act -> dtoActions.stream().noneMatch(dto -> Objects.equals(dto.getAction(), act.getAction()) && Objects.equals(dto.getOutput(), act.getOutput())));
                    // Обновляем существующие и добавляем новые действия
                    for (RCSchedulerDTO.RCTaskActionDTO dtoAction : dtoActions) {
                        RCTaskAction action = existingActions.stream()
                            .filter(a -> Objects.equals(dtoAction.getAction(), a.getAction()) && Objects.equals(dtoAction.getOutput(), a.getOutput()))
                            .findFirst()
                            .orElse(null);
                        if (action == null) {
                            action = new RCTaskAction();
                            action.setTask(task);
                            existingActions.add(action);
                        }
                        action.setAction(dtoAction.getAction());
                        action.setOutput(dtoAction.getOutput());
                        action.setType(dtoAction.getType());
                        action.setInput(dtoAction.getInput());
                    }
                }
                rcSchedulerRepository.save(existingScheduler);
                return "OK";
            }

            // если нет существующего планировщика, создаём новый
            RCScheduler rcScheduler = rcSchedulerMapper.toEntity(rcSchedulerDTO);
            rcScheduler.setController(relayController);
            rcSchedulerRepository.save(rcScheduler);
        } catch (Exception e) {
            logger.error("Error while saving scheduler config", e);
            return e.getLocalizedMessage();
        }
        return "OK";
    }

    @Transactional
    public void linkNodeRC(String mac, String nodeMac, RCModel model) {
        RelayController rc = relayControllerRepository.findByMac(mac);
        RelayController nodeRc = checkCreateRC(nodeMac, model);

        if (rc == null || nodeRc == null) {
            throw new RuntimeException("RC not found");
        }
        if (nodeRc.getUser() == null) {
            nodeRc.setUser(rc.getUser());
        }

        RCGroup rcGroup = rc.getGroup();
        RCGroup nodeGroup = nodeRc.getGroup();

        if (rcGroup != null && rcGroup.equals(nodeGroup)) {
            return;
        }
        if (rcGroup != null && nodeGroup == null) {
            nodeRc.setGroup(rcGroup);
            return;
        }
        if (rcGroup == null && nodeGroup != null) {
            rc.setGroup(nodeGroup);
            return;
        }
        if (rcGroup == null && nodeGroup == null) {
            RCGroup g = new RCGroup();
            rc.setGroup(g);
            nodeRc.setGroup(g);
            return;
        }

        // --- CASE 4: обе имеют разные группы (самое важное) ---
        // тут нужно объединять группы
        // переносим все контроллеры из g2 в g1
        for (RelayController rcont : nodeGroup.getControllers()) {
            rcont.setGroup(rcGroup);
        }

        // TODO : delete empty groups
    }

    public RelayController checkCreateRC(String mac, RCModel model) {
        RelayController rc = relayControllerRepository.findByMac(mac);
        if (rc != null) {
            return rc;
        }
        return createDefaultRC(mac, model);
    }

    public RelayController createDefaultRC(String mac, RCModel model) {
        if (mac == null) return null;

        RelayController rc = new RelayController();
        rc.setMac(mac);
        rc.setName("new RC "+mac);
        rc.setDescription("my RC "+mac);
        rc.setType("relayController");
        rc.setModel(model.name());

        List<RCOutput> outputs = new ArrayList<>();
        for (int i = 0; i < model.getOutputs(); i++) {
            RCOutput out = new RCOutput();
            out.setId(i);
            out.setType(RCOutType.s);
            out.setName("OUT-" + i);
            out.setRelayController(rc);
            outputs.add(out);
        }

        List<RCInput> inputs = new ArrayList<>();
        for (int i = 0; i < model.getInputs(); i++) {
            RCInput in = new RCInput();
            in.setId(i);
            in.setName("IN-" + i);
            in.setType(RCInType.s);
            in.setRelayController(rc);

            // если есть соответствующий выход
            if (i < outputs.size()) {
                RCEvent onEvent = new RCEvent();
                onEvent.setEvent("on");
                onEvent.setInput(in);

                RCEvent offEvent = new RCEvent();
                offEvent.setEvent("off");
                offEvent.setInput(in);

                RCAction onAction = new RCAction();
                onAction.setOrder(0);
                onAction.setAction("on");
                onAction.setOutput(outputs.get(i));
                onAction.setEvent(onEvent);

                RCAction offAction = new RCAction();
                offAction.setOrder(0);
                offAction.setAction("off");
                offAction.setOutput(outputs.get(i));
                offAction.setEvent(offEvent);

                onEvent.setActions(List.of(onAction));
                offEvent.setActions(List.of(offAction));

                in.setEvents(List.of(onEvent, offEvent));
            }

            inputs.add(in);
        }

        for (int i = 0; i < model.getButtons(); i++) {
            RCInput btn = new RCInput();
            btn.setId(16 + i); // чтобы не пересекались с switch
            btn.setName("BTN-" + i);
            btn.setType(RCInType.b);
            btn.setRelayController(rc);

            if (i < outputs.size()) {
                RCEvent toggleEvent = new RCEvent();
                toggleEvent.setEvent("toggle");
                toggleEvent.setInput(btn);

                RCAction toggleAction = new RCAction();
                toggleAction.setOrder(0);
                toggleAction.setAction("toggle");
                toggleAction.setOutput(outputs.get(i));
                toggleAction.setEvent(toggleEvent);

                toggleEvent.setActions(List.of(toggleAction));
                btn.setEvents(List.of(toggleEvent));
            }

            inputs.add(btn);
        }

        rc.setInputs(inputs);
        rc.setOutputs(outputs);
        relayControllerRepository.save(rc);
        return rc;
    }

    @Transactional
    public String saveNewRelayController(RCConfigDTO rcConfigDTO) throws InvocationTargetException, IllegalAccessException {
        // only for new RC
        // find existing RC
        String mac = rcConfigDTO.getMac();
        String res = "OK";
        if (mac == null) {
            res = "Mac is null";
            logger.error(res);
            return res;
        }
        RelayController existingRelayController = relayControllerRepository.findByMac(mac);
        if (existingRelayController != null) {
            // rc found. delete it
            logger.info("found existed relayController. deleting it");
            relayControllerRepository.delete(existingRelayController);
        }

        RelayController relayController = relayControllerMapper.toEntity(rcConfigDTO); //rcConfigMapper.toEntityRC(rcConfigDTO);
        relayControllerRepository.save(relayController);

        // save config
        res = saveConfig(rcConfigDTO);
        return res;
    }

    public User getUser(String mac) {
        RelayController c = relayControllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            return c.getUser();
        }
        return null;
    }

    @Transactional
    public String updateOutput(RCOutputDTO rcOutputDTO) {
        // обновление сущности выхода с фронта
        Optional<RCOutput> rcOutputOpt = outputRepository.findById(rcOutputDTO.getUuid());

        if (rcOutputOpt.isPresent()) {
            RCOutput rcOutput = rcOutputOpt.get();
            rcOutput.setName(rcOutputDTO.getName());
            rcOutput.setAlice(rcOutputDTO.getAlice());
            rcOutput.setRoom(rcOutputDTO.getRoom());
            rcOutput.setTimer(rcOutputDTO.getTimer());
            rcOutput.setOff(rcOutputDTO.getOff());
            rcOutput.setOn(rcOutputDTO.getOn());
            //rcOutput.setType(rcOutputDTO.getType());
            rcOutput.setLimit(rcOutputDTO.getLimit());
            rcOutput.set_default(rcOutputDTO.get_default());
            outputRepository.save(rcOutput);
            return "OK";
        } else {
            return "Output not found";
        }
    }

    @Transactional
    private String updateInputTransaction1(RCInputDTO rcInputDTO) {
        Optional<RCInput> rcInputOpt = inputRepository.findById(rcInputDTO.getUuid());
        if (rcInputOpt.isEmpty()) {
            return "INPUT_NOT_FOUND";
        }

        RCInput rcInput = rcInputOpt.get();
        rcInput.setName(rcInputDTO.getName());
        //rcInput.setType(rcInputDTO.getType());

        if (!rcInput.getCRC().equals(rcInputDTO.getCRC())) {
            // events changed
            rcInput.getEvents().clear();
            inputRepository.flush();

            if (rcInputDTO.getEvents() != null) {
                for (RCEventDTO eventDto : rcInputDTO.getEvents()) {
                    RCEvent event = new RCEvent();
                    event.setEvent(eventDto.getEvent());
                    event.setInput(rcInput);

                    // actions
                    if (eventDto.getActions() != null) {
                        List<RCAction> actions = new ArrayList<>();
                        for (RCActionDTO actionDto : eventDto.getActions()) {
                            RCAction action = getActionFromDto(actionDto);
                            action.setEvent(event);
                            actions.add(action);
                        }
                        event.setActions(actions);
                    }
                    // acls
                    if (eventDto.getAcls() != null) {
                        List<RCAcl> acls = new ArrayList<>();
                        for (RCAclDTO aclDto : eventDto.getAcls()) {
                            RCAcl acl = getAclFromDto(aclDto);
                            acl.setEvent(event);
                            acls.add(acl);
                        }
                        event.setAcls(acls);
                    }
                    rcInput.getEvents().add(event);
                }
            }
        } else {
            logger.info("Input {} events not changed", rcInput.getName());
        }
        inputRepository.save(rcInput);
        return "OK";
    }

    private RCAction getActionFromDto(RCActionDTO actionDto) {
        RCAction action = new RCAction();
        action.setOutput(outputRepository.getReferenceById(actionDto.getOutputUuid()));
        action.setOrder(actionDto.getOrder());
        action.setAction(actionDto.getAction());
        action.setDuration(actionDto.getDuration());
        return action;
    }

    private static RCAcl getAclFromDto(RCAclDTO aclDto) {
        RCAcl acl = new RCAcl();
        acl.setType(aclDto.getType());
        acl.setId(aclDto.getId());
        acl.setIo(aclDto.getIo());
        acl.setState(aclDto.getState());
        return acl;
    }

    public String getDeviceActionMessage(RCOutput output, String action) {
        // for alice
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("type", "ACTION");
        objectMap.put("payload", new HashMap<String, Object>() {{
            put("output", output.getId());
            put("action", action);
        }});
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(objectMap);
            logger.info(json);
            return json;
        } catch (JsonProcessingException e) {
            logger.error(e.getLocalizedMessage());
        }
        return "{}";
    }

    @Transactional
    //private String updateInputTransaction(RCInputDTO rcInputDTO) {
    public String updateInput(RCInputDTO rcInputDTO) {
        Optional<RCInput> rcInputOpt = inputRepository.findById(rcInputDTO.getUuid());
        if (rcInputOpt.isEmpty()) {
            return "INPUT_NOT_FOUND";
        }

        RCInput rcInput = rcInputOpt.get();
        rcInput.setName(rcInputDTO.getName());
        //rcInput.setType(rcInputDTO.getType());
        mergeEvents(rcInput, rcInputDTO.getEvents());

        inputRepository.save(rcInput);
        return "OK";
    }

    private void mergeEvents(RCInput input, List<RCEventDTO> eventDTOs) {

        Map<String, RCEvent> existingEvents = input.getEvents()
                .stream()
                .collect(Collectors.toMap(RCEvent::getEvent, e -> e));

        List<RCEvent> newEvents = new ArrayList<>();
        for (RCEventDTO dto : eventDTOs) {
            RCEvent event;
            if (dto.getEvent() != null && existingEvents.containsKey(dto.getEvent())) {
                event = existingEvents.remove(dto.getEvent());
            } else {
                event = new RCEvent();
                event.setInput(input);
                event.setEvent(dto.getEvent());
            }
            mergeActions(event, dto.getActions());
            newEvents.add(event);
        }
        existingEvents.values().forEach(ev -> input.getEvents().remove(ev));

        input.getEvents().clear();
        input.getEvents().addAll(newEvents);
    }

    private void mergeActions(RCEvent event, List<RCActionDTO> dtos) {
        // always replace actions
        if (event.getActions() != null)
            event.getActions().clear();

        List<RCAction> actions = new ArrayList<>();
        for (RCActionDTO actionDto : dtos) {
            RCAction action = getActionFromDto(actionDto);
            action.setEvent(event);
            actions.add(action);
        }
        event.setActions(actions);

    }

    @Transactional
    public String getIOStates(String mac) {
        RelayController relayController = relayControllerRepository.findByMac(mac);
        if (relayController == null)
            return "{}";
        RCUpdateIODTO rcUpdateIODTO = relayControllerMapper.getRCStates(relayController);

        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("type", "UPDATE");
        objectMap.put("payload", rcUpdateIODTO);
        return Utils.getJson(objectMap);
    }

    public List<RelayController> resolveGroupControllers(RelayController rc) {
        if (rc.getGroup() == null) {
            return List.of(rc);
        }
        return rc.getGroup().getControllers();
    }

    private void writeMac(ByteBuffer buf, String macStr) {
        if (macStr == null)
            throw new IllegalArgumentException("MAC is null");

        String clean = macStr.replace(":", "").replace("-", "").trim();

        if (clean.length() != 12)
            throw new IllegalArgumentException("Bad MAC: " + macStr);

        for (int i = 0; i < 6; i++) {
            int b = Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
            buf.put((byte) b);
        }
    }

    private void writeAction(ByteBuffer buf, RCAction a, List<RelayController> all) {
        RelayController targetNode = a.getNode();
        if (targetNode == null)
            throw new IllegalStateException("Action has no target node");

        RCOutput out = a.getOutput();
        writeMac(buf, targetNode.getMac());
        buf.put(out.getId().byteValue());
        buf.put(mapAction(a.getAction()));
        int duration = (a.getDuration() == null) ? 0 : a.getDuration();
        buf.putShort((short) duration);
    }

    private void writeOutput(ByteBuffer buf, RCOutput o) {

        buf.put(o.getId().byteValue());                          // id
        buf.put(BConfigMapper.mapOutputType(o.getType()));          // type

        buf.put((byte) ("on".equalsIgnoreCase(o.getState()) ? 1 : 0)); // is_on
        buf.put((byte) ("1".equals(o.get_default()) ? 1 : 0));         // def_value

        buf.putShort((short) (o.getTimer() == null ? 0 : o.getTimer())); // timer

        switch (o.getType()) {
            case s -> {
                buf.putShort((short) (o.getLimit() == null ? 0 : o.getLimit()));
                buf.putShort((short) 0); // выравниваем под union размер
            }
            case t -> {
                buf.putShort((short) (o.getOn() == null ? 0 : o.getOn()));
                buf.putShort((short) (o.getOff() == null ? 0 : o.getOff()));
            }
            default -> {
                buf.putShort((short) 0);
                buf.putShort((short) 0);
            }
        }
    }

    private void writeInput(ByteBuffer buf, RCInput in) {
        buf.put(in.getId().byteValue());
        buf.put(BConfigMapper.mapInputType(in.getType()));
        buf.put((byte) ("on".equalsIgnoreCase(in.getState()) ? 1 : 0));

        List<RCEvent> events = in.getEvents();

        buf.put((byte) events.size());

        int offsetPos = buf.position();
        buf.putShort((short) 0); // временно

        // сохраним позицию чтобы позже дописать offset
        inputEventOffsetPatchList.add(new Patch(offsetPos, events));
    }

    private void writeEvent(ByteBuffer buf, RCEvent e) {
        buf.put(BConfigMapper.mapEvent(e.getEvent()));

        List<RCAction> actions = e.getActions();
        buf.put((byte) actions.size());

        int offsetPos = buf.position();
        buf.putShort((short) 0);

        eventActionOffsetPatchList.add(new PatchAction(offsetPos, actions));
    }

    private void writeNode(ByteBuffer buf, RelayController rc, List<RelayController> all) {
        writeMac(buf, rc.getMac());

        int ioCfgPos = buf.position();
        buf.position(ioCfgPos + 16); // reserve io_cfg_t

        int outputsOffset = buf.position();
        rc.getOutputs().forEach(o -> writeOutput(buf, o));

        int inputsOffset = buf.position();
        rc.getInputs().forEach(i -> writeInput(buf, i));

        int eventsOffset = buf.position();
        rc.getInputs().forEach(i -> i.getEvents().forEach(e -> writeEvent(buf, e)));

        int actionsOffset = buf.position();
        rc.getInputs().forEach(i ->
                i.getEvents().forEach(e ->
                        e.getActions().forEach(a -> writeAction(buf, a, all))));

        int cur = buf.position();

        // записываем io_cfg
        buf.position(ioCfgPos);
        buf.putInt(outputsOffset);
        buf.putInt(inputsOffset);
        buf.putInt(eventsOffset);
        buf.putInt(actionsOffset);
        buf.put((byte) rc.getOutputs().size());
        buf.put((byte) rc.getInputs().size());
        buf.putShort((short) rc.getInputs().stream().mapToInt(i -> i.getEvents().size()).sum());
        buf.putShort((short) rc.getInputs().stream()
                .flatMap(i -> i.getEvents().stream())
                .mapToInt(e -> e.getActions().size()).sum());

        buf.position(cur);

        // после записи всех outputs/inputs/events/actions
        int actionsStart = actionsOffset;

        for (PatchAction p : eventActionOffsetPatchList) {
            int currentPos = buf.position();
            buf.position(p.pos());
            buf.putShort((short) actionsStart);
            buf.position(currentPos);

            for (RCAction a : p.actions()) {
                writeAction(buf, a, all);
            }
        }

        int eventsStart = eventsOffset;

        for (Patch p : inputEventOffsetPatchList) {
            int currentPos = buf.position();
            buf.position(p.pos());
            buf.putShort((short) eventsStart);
            buf.position(currentPos);

            for (RCEvent e : p.events()) {
                writeEvent(buf, e);
            }
        }
    }

    public BinaryMessage makeBConfig(String mac) {
        RelayController relayController = relayControllerRepository.findByMac(mac);
        if (relayController == null)
            return null;

        List<RelayController> controllers = resolveGroupControllers(relayController);

        ByteBuffer buf = ByteBuffer.allocate(8192);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        int nodesPos = buf.position();

        // header nodes_cfg_t
        buf.put((byte) controllers.size()); // nodes_count
        int nodesArrayPos = buf.position();
        buf.position(nodesArrayPos + controllers.size() * 4); // reserve pointers

        List<Integer> nodeOffsets = new ArrayList<>();

        for (RelayController rc : controllers) {
            nodeOffsets.add(buf.position());
            writeNode(buf, rc, controllers);
        }

        int end = buf.position();

        // прописываем offsets
        buf.position(nodesArrayPos);
        nodeOffsets.forEach(buf::putInt);

        byte[] data = Arrays.copyOf(buf.array(), end);
        return new BinaryMessage(data);
    }

    public BinaryMessage getCmdMessage(String msg) {
        byte[] data = new byte[5];
        data[0] = (byte) 0xAA;
        data[1] = (byte) 0xC0;

        byte cmd = switch (msg.toUpperCase()) {
            case "OTA" -> 'O';
            case "INFO" -> 'I';
            case "REBOOT" -> 'R';
            case "LOGS" -> 'L';
            default -> 'N'; // nothing
        };

        data[2] = cmd;
        int crc = Utils.crc16(data, 3);
        data[3] = (byte) (crc & 0xFF);
        data[4] = (byte) ((crc >> 8) & 0xFF);

        return new BinaryMessage(data);
    }

    public BinaryMessage getActionMessage(String node, Integer output, String action) {
        byte[] data = new byte[12];
        data[0] = (byte) 0xAA;
        data[1] = (byte) 0xAC;

        if (node == null || node.length() != 12)
            return null;

        for (int i = 0; i < node.length(); i += 2) {
            String hexPair = node.substring(i, i + 2);
            data[2 + (i / 2)] = (byte) Integer.parseInt(hexPair, 16);
        }
        data[8] = output.byteValue();
        data[9] = mapAction(action);

        int crc = Utils.crc16(data, 10);
        data[10] = (byte) (crc & 0xFF);
        data[11] = (byte) ((crc >> 8) & 0xFF);

        return new BinaryMessage(data);
    }
}
