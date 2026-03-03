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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
    public void updateIOStates(String mac, int outputsStates, int inputsStates) {
        RelayController relayController = relayControllerRepository.findByMac(mac);
        if (relayController == null)
            return;
        relayController.getOutputs().forEach(output -> {
            int id = output.getId();
            boolean state = (outputsStates & (1 << id)) != 0;
            output.setState(state ? "on" : "off");
        });

        relayController.getInputs().forEach(input -> {
            int id = input.getId();
            boolean state = (inputsStates & (1 << id)) != 0;
            input.setState(state ? "on" : "off");
        });
        relayControllerRepository.save(relayController);
    }

    public void setOutputState(String mac, Integer output, String state) {
        RelayController c = relayControllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            RCOutput o = outputRepository.findOutput(c.getUuid(), output);
            if (o != null) {
                o.setState(state);
                outputRepository.save(o);
            }
            controllerService.updateLastSeen(c);
        }
    }

    public void setInputState(String mac, Integer input, String state) {
        RelayController c = relayControllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            RCInput o = inputRepository.findInput(c.getUuid(), input);
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
    public void linkNodeRC(String mac, String nodeMac, CModel model) {
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

    public RelayController checkCreateRC(String mac, CModel model) {
        RelayController rc = relayControllerRepository.findByMac(mac);
        if (rc != null) {
            return rc;
        }
        return createDefaultRC(mac, model);
    }

    public RelayController createDefaultRC(String mac, CModel model) {
        if (mac == null) return null;

        RelayController rc = new RelayController();
        rc.setMac(mac);
        rc.setName("new RC "+mac);
        rc.setDescription("my RC "+mac);
        rc.setType("relayController");
        rc.setModel(model);

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
        if (event.getActions() == null) {
            event.setActions(new ArrayList<>());
        }
        event.getActions().clear();
        if (dtos != null) {
            for (RCActionDTO actionDto : dtos) {
                RCAction action = getActionFromDto(actionDto);
                action.setEvent(event);
                event.getActions().add(action);
            }
        }
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
        RCGroup g = rc.getGroup();
        if (g == null) {
            return List.of(rc);
        }
        return g.getControllers();
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

//    private void writeMac(DataOutputStream dos, String macStr) throws IOException {
//        if (macStr == null) throw new IllegalArgumentException("MAC is null");
//        String clean = macStr.replace(":", "").replace("-", "").trim();
//        if (clean.length() != 12) throw new IllegalArgumentException("Bad MAC: " + macStr);
//        for (int i = 0; i < 6; i++) {
//            int b = Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
//            dos.writeByte(b);
//        }
//    }

//    private void writeAction2(ByteBuffer buf, RCAction a, List<RelayController> all) {
//        RelayController targetNode = a.getNode();
//        if (targetNode == null) {
//            // if no specific node
//            targetNode = a.getEvent().getInput().getRelayController();
//            //throw new IllegalStateException("Action has no target node");
//        }
//        RCOutput out = a.getOutput();
//        writeMac(buf, targetNode.getMac());
//        buf.put(out.getId().byteValue());
//        buf.put(mapAction(a.getAction()));
//        int duration = (a.getDuration() == null) ? 0 : a.getDuration();
//        buf.putShort((short) duration);
//    }
//
//    private void writeOutput2(ByteBuffer buf, RCOutput o) {
//
//        buf.put(o.getId().byteValue());                          // id
//        buf.put(BConfigMapper.mapOutputType(o.getType()));          // type
//
//        buf.put((byte) ("on".equalsIgnoreCase(o.getState()) ? 1 : 0)); // is_on
//        buf.put((byte) ("1".equals(o.get_default()) ? 1 : 0));         // def_value
//
//        buf.putShort((short) (o.getTimer() == null ? 0 : o.getTimer())); // timer
//
//        switch (o.getType()) {
//            case s -> {
//                buf.putShort((short) (o.getLimit() == null ? 0 : o.getLimit()));
//                buf.putShort((short) 0); // выравниваем под union размер
//            }
//            case t -> {
//                buf.putShort((short) (o.getOn() == null ? 0 : o.getOn()));
//                buf.putShort((short) (o.getOff() == null ? 0 : o.getOff()));
//            }
//            default -> {
//                buf.putShort((short) 0);
//                buf.putShort((short) 0);
//            }
//        }
//    }
//
//    private void writeInput2(ByteBuffer buf, RCInput in) {
//        buf.put(in.getId().byteValue());
//        buf.put(BConfigMapper.mapInputType(in.getType()));
//        buf.put((byte) ("on".equalsIgnoreCase(in.getState()) ? 1 : 0));
//
//        List<RCEvent> events = in.getEvents();
//
//        buf.put((byte) events.size());
//
//        int offsetPos = buf.position();
//        buf.putShort((short) 0); // временно
//
//        // сохраним позицию чтобы позже дописать offset
//        inputEventOffsetPatchList.add(new Patch(offsetPos, events));
//    }
//
//    private void writeEvent2(ByteBuffer buf, RCEvent e) {
//        buf.put(BConfigMapper.mapEvent(e.getEvent()));
//
//        List<RCAction> actions = e.getActions();
//        buf.put((byte) actions.size());
//
//        int offsetPos = buf.position();
//        buf.putShort((short) 0);
//
//        eventActionOffsetPatchList.add(new PatchAction(offsetPos, actions));
//    }

//    private void writeNode2(ByteBuffer buf, RelayController rc, List<RelayController> all) {
//        // записываем MAC
//        writeMac(buf, rc.getMac());
//
//        // резервируем место для io_cfg_t заголовка
//        int ioCfgPos = buf.position();
//        buf.position(ioCfgPos + 8); // version(2) + outputs_count(1) + inputs_count(1) + events_count(2) + actions_count(2) = 8 байт
//
//        // пишем outputs
//        rc.getOutputs().forEach(o -> writeOutput(buf, o));
//
//        // пишем inputs
//        rc.getInputs().forEach(i -> writeInput(buf, i));
//
//        // пишем events
//        rc.getInputs().forEach(i -> i.getEvents().forEach(e -> writeEvent(buf, e)));
//
//        // пишем actions
//        rc.getInputs().forEach(i ->
//                i.getEvents().forEach(e ->
//                        e.getActions().forEach(a -> writeAction(buf, a, all))
//                )
//        );
//
//        // считаем количество элементов
//        int outputsCount = rc.getOutputs().size();
//        int inputsCount = rc.getInputs().size();
//        int eventsCount = rc.getInputs().stream()
//                .mapToInt(i -> i.getEvents().size())
//                .sum();
//        int actionsCount = rc.getInputs().stream()
//                .flatMap(i -> i.getEvents().stream())
//                .mapToInt(e -> e.getActions().size())
//                .sum();
//
//        // возвращаемся и записываем заголовок io_cfg_t
//        buf.position(ioCfgPos);
//        buf.putShort((short) 10); // версия конфига, например 1
//        buf.put((byte) outputsCount);
//        buf.put((byte) inputsCount);
//        buf.putShort((short) eventsCount);
//        buf.putShort((short) actionsCount);
//
//        // возвращаемся в конец для дальнейшей записи
//        //buf.position(buf.limit());
//    }

    private void writeOutput(ByteBuffer buf, RCOutput o) {
        // 10 bytes
        buf.put(o.getId().byteValue());                                // id
        buf.put(BConfigMapper.mapOutputType(o.getType()));
        buf.put((byte) ("on".equalsIgnoreCase(o.getState()) ? 1 : 0)); // is_on
        buf.put((byte) ("on".equals(o.get_default()) ? 1 : 0));          // def_value
        buf.putShort(o.getTimer() == null ? 0 : o.getTimer().shortValue());

        switch (o.getType()) {
            case s -> { // simple
                buf.putShort(o.getLimit() == null ? 0 : o.getLimit().shortValue());
                buf.putShort((short) 0); // padding
            }
            case t -> { // timer
                buf.putShort((short) (o.getOn() == null ? 0 : o.getOn()));
                buf.putShort((short) (o.getOff() == null ? 0 : o.getOn()));
            }
            default -> {
                buf.putShort((short) 0);
                buf.putShort((short) 0);
            }
        }
    }

    private short writeInput(ByteBuffer buf, RCInput in, short offset) {
        // 6 bytes
        //uint8_t id;
        //input_type_t type;
        //bool is_on;
        //uint8_t events_count;
        //uint16_t events_offset;
        byte size = (byte)in.getEvents().size();
        buf.put(in.getId().byteValue());
        buf.put(BConfigMapper.mapInputType(in.getType()));
        buf.put((byte) ("on".equalsIgnoreCase(in.getState()) ? 1 : 0));
        buf.put(size); // events_count
        buf.putShort(offset);
        return (short) (offset + size);
    }

    private short writeEvent(ByteBuffer buf, RCEvent e, short offset) {
        //event_type_t event;
        //uint8_t actions_count;
        //uint16_t actions_offset;

        byte actionsCount = (byte)e.getActions().size();
        buf.put(BConfigMapper.mapEvent(e.getEvent()));
        buf.put(actionsCount);
        buf.putShort(offset);
        offset += (short) actionsCount;

        return offset;
    }

    private void writeAction(ByteBuffer buf, RCAction a) {
        //node_uid_t target_node;
        //uint8_t output_id;
        //action_type_t action;
        //uint16_t duration_sec;
        RCOutput out = a.getOutput();
        if (out == null) {
            // ???
            return;
        }
        String node = out.getRelayController().getMac();
        writeMac(buf, node);
        byte outputId = out.getId().byteValue();
        buf.put(outputId);
        buf.put(mapAction(a.getAction()));
        buf.putShort((short)(a.getDuration() == null ? 0 : a.getDuration()));
    }

    private void writeNode(ByteBuffer buf, RelayController rc) {
        // UID
        writeMac(buf, rc.getMac()); // 6 байт

        // io_cfg_t header
        Integer version = rc.getVersion();
        if (version == null)
            version = 1;
        buf.putShort((short)(version.shortValue() + 1)); // version
        buf.put((byte) rc.getOutputs().size());
        buf.put((byte) rc.getInputs().size());

        int eventsCount = rc.getInputs().stream().mapToInt(i -> i.getEvents().size()).sum();
        int actionsCount = rc.getInputs().stream()
                .flatMap(i -> i.getEvents().stream())
                .mapToInt(e -> e.getActions().size())
                .sum();
        logger.info("mac {}, events {}, actions {}", rc.getMac(), eventsCount, actionsCount);

        buf.putShort((short) eventsCount);
        buf.putShort((short) actionsCount);

        // write outputs
        for (RCOutput o : rc.getOutputs())
            writeOutput(buf, o);

        ByteBuffer bInputs = ByteBuffer.allocate(6 * 32);
        bInputs.order(ByteOrder.LITTLE_ENDIAN);
        short oInputs = 0;

        ByteBuffer bEvents = ByteBuffer.allocate(4 * (16*4));
        bEvents.order(ByteOrder.LITTLE_ENDIAN);
        short oEvents = 0;

        ByteBuffer bActions = ByteBuffer.allocate(10 * 4 * 256); // too many
        bActions.order(ByteOrder.LITTLE_ENDIAN);

        // write inputs
        for (RCInput i : rc.getInputs()) {
            oInputs = writeInput(bInputs, i, oInputs);
            if (!i.getEvents().isEmpty()) {
                for (RCEvent e : i.getEvents()) {
                    // тут додумать, чтобы если нет экшенов, то не передавать пустышки
                    oEvents = writeEvent(bEvents, e, oEvents);
                    for (RCAction a : e.getActions()) {
                        writeAction(bActions, a);
                    }
                }
            }
        }

        buf.put(buf.position(), bInputs, 0, bInputs.position());
        buf.position(buf.position() + bInputs.position());
        buf.put(buf.position(), bEvents, 0, bEvents.position());
        buf.position(buf.position() + bEvents.position());
        buf.put(buf.position(), bActions, 0, bActions.position());
        buf.position(buf.position() + bActions.position());
    }

    @Transactional
    public BinaryMessage makeBConfig(String mac) {
        RelayController relayController = relayControllerRepository.findByMac(mac);
        if (relayController == null) return null;

        List<RelayController> controllers = resolveGroupControllers(relayController);

        ByteBuffer buf = ByteBuffer.allocate(8192 * 10);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put((byte)0xAA);
        buf.put((byte)'C');
        // префикс
        buf.put((byte)controllers.size());

        // пишем все контроллеры
        for (RelayController rc : controllers) {
            writeNode(buf, rc);
        }

        byte[] dataForCrc = Arrays.copyOf(buf.array(), buf.position());
        int crc = Utils.crc16(dataForCrc, dataForCrc.length);

        buf.putShort((short) crc);

        byte[] data = Arrays.copyOf(buf.array(), buf.position());

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

    public BinaryMessage getActionMessage(ActionDTO actionDTO) {
        if (actionDTO.getAction() == null ||
                actionDTO.getNode() == null || actionDTO.getNode().length() != 12 ||
                (actionDTO.getOutput() == null && actionDTO.getInput() == null))
            return null;
        // 0   1  2-7   8       9     10    11-12
        // AA  A  NODE  OUT/IN  ID  ACTION  CRC16

        byte[] data = new byte[13];
        data[0] = (byte) 0xAA;
        data[1] = (byte) 'A';

        for (int i = 0; i < actionDTO.getNode().length(); i += 2) {
            String hexPair = actionDTO.getNode().substring(i, i + 2);
            data[2 + (i / 2)] = (byte) Integer.parseInt(hexPair, 16);
        }
        if (actionDTO.getOutput() != null) {
            data[8] = 0;
            data[9] = actionDTO.getOutput().byteValue();
        } else {
            data[8] = 1;
            data[9] = actionDTO.getInput().byteValue();
        }
        data[10] = mapAction(actionDTO.getAction());

        int crc = Utils.crc16(data, 11);
        data[11] = (byte) (crc & 0xFF);
        data[12] = (byte) ((crc >> 8) & 0xFF);

        return new BinaryMessage(data);
    }

    public RelayController findRelayController(String mac) {
        return relayControllerRepository.findByMac(mac);
    }
}
