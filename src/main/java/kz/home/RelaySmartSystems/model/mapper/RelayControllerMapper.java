package kz.home.RelaySmartSystems.model.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.dto.*;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.*;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RelayControllerMapper {
    private final ObjectMapper mapper = new ObjectMapper();

    private RCActionDTO mapAction(RCAction action) {
        return new RCActionDTO(
                action.getUuid(),
                action.getOrder(),
                action.getOutput().getId(),
                action.getAction(),
                action.getDuration(),
                action.getOutput().getUuid()
        );
    }

    private RCAclDTO mapAcl(RCAcl acl) {
        return new RCAclDTO(
                acl.getUuid(),
                acl.getType(),
                acl.getId(),
                acl.getIo(),
                acl.getState()
        );
    }

    private RCEventDTO mapEvent(RCEvent event) {
        List<RCActionDTO> actions = event.getActions().stream()
                .map(this::mapAction)
                .collect(Collectors.toList());

        List<RCAclDTO> acls = event.getAcls().stream()
                .map(this::mapAcl)
                .collect(Collectors.toList());

        return new RCEventDTO(
                event.getUuid(),
                event.getEvent(),
                actions,
                acls
        );
    }

    private RCInputDTO mapInput(RCInput input) {
        return new RCInputDTO(
                input.getUuid(),
                input.getId(),
                input.getName(),
                input.getType().getValue(),
                input.getState(),
                input.getEvents().stream()
                        .map(this::mapEvent)
                        .collect(Collectors.toList())
        );
    }

    public List<RCOutputDTO> outputsToDTO(List<RCOutput> outputs) {
        return new ArrayList<>(outputs.stream()
                .map(o -> new RCOutputDTO(o.getUuid(), o.getId(), o.getName(), o.getLimit(), o.getType().getValue(),
                        o.get_default(), o.getState(), o.getAlice(), o.getRoom(), o.getOn(), o.getOff()))
                .toList());
    }

    public List<RCInputDTO> inputsToDTO(List<RCInput> inputs) {
        return new ArrayList<>(inputs.stream()
                .map(this::mapInput)
                .toList());
    }

    public RelayController toEntity(RCConfigDTO rcConfigDTO) throws InvocationTargetException, IllegalAccessException {
        // when config received from device
        RelayController relayController = new RelayController();

        // general info
        relayController.setMac(rcConfigDTO.getMac());
        relayController.setName(rcConfigDTO.getName());
        relayController.setDescription(rcConfigDTO.getDescription());
        relayController.setModel(rcConfigDTO.getModel());
        relayController.setType("relayController");
        //relayController.setCrc(rcConfigDTO.getCrc());
        //relayController.setHwParams(Utils.getJson(rcConfigDTO.getHwParams()));
//        try {
//            String json = mapper.writeValueAsString(rcConfigDTO.getHwParams());
//            relayController.setHwParams(json);
//        } catch (Exception ignored) {
//        }

        // outputs
        List<RCOutput> outputs = new ArrayList<>();
        for (RCOutputDTO outputDTO : rcConfigDTO.getIo().getOutputs()) {
            RCOutput output = new RCOutput();
            output.setRelayController(relayController);
            BeanUtils.copyProperties(output, outputDTO);
            outputs.add(output);
        }
        relayController.setOutputs(outputs);

        // inputs
        List<RCInput> inputs = new ArrayList<>();
        for (RCInputDTO inputDTO : rcConfigDTO.getIo().getInputs()) {
            RCInput input = new RCInput();
            input.setRelayController(relayController);
            //BeanUtils.copyProperties(input, inputDTO); // Cannot invoke kz.home.RelaySmartSystems.model.entity.relaycontroller.RCInput.setEvents - argument type mismatch
            input.setId(inputDTO.getId());
            input.setName(inputDTO.getName());
            input.setType(RCInType.fromCode(inputDTO.getType()));
//            if (input.getType() == null)
//                input.setType("SW");
            input.setState(inputDTO.getState());

            if (inputDTO.getEvents() != null) {
                List<RCEvent> newEvents = new ArrayList<>();
                for (RCEventDTO eventDTO : inputDTO.getEvents()) {
                    RCEvent newEvent = new RCEvent();
                    BeanUtils.copyProperties(newEvent, eventDTO);
                    newEvent.setInput(input);
                    // actions
                    if (eventDTO.getActions() != null) {
                        List<RCAction> newActions = new ArrayList<>();
                        for (RCActionDTO action : eventDTO.getActions()) {
                            RCAction newAction = new RCAction();
                            //BeanUtils.copyProperties(newAction, action);
                            newAction.setAction(action.getAction());
                            newAction.setDuration(action.getDuration());
                            newAction.setOrder(action.getOrder());
                            RCOutput out = outputs.stream().filter(rcOutput -> rcOutput.getId().equals(action.getOutput()))
                                    .findFirst().orElse(null);
                            newAction.setOutput(out);
                            newAction.setEvent(newEvent);
                            newActions.add(newAction);
                        }
                        newEvent.setActions(newActions);
                    }
                    // acls
                    if (eventDTO.getAcls() != null) {
                        List<RCAcl> newAcls = new ArrayList<>();
                        for (RCAclDTO acl : eventDTO.getAcls()) {
                            RCAcl newAcl = new RCAcl();
                            BeanUtils.copyProperties(newAcl, acl);
                            newAcl.setEvent(newEvent);
                            newAcls.add(newAcl);
                        }
                        newEvent.setAcls(newAcls);
                    }
                    newEvents.add(newEvent);
                }
                input.setEvents(newEvents);
            }
            inputs.add(input);
        }
        relayController.setInputs(inputs);

        return relayController;
    }

    public RCUpdateIODTO getRCStates(RelayController relayController) {
        RCUpdateIODTO rcUpdateIODTO = new RCUpdateIODTO();
        rcUpdateIODTO.setMac(relayController.getMac());
        List<RCUpdateIODTO.RCState> rcOutputStates = new ArrayList<>();
        for (RCOutput rcOutput : relayController.getOutputs()) {
            RCUpdateIODTO.RCState rcState = new RCUpdateIODTO.RCState();
            rcState.setState(rcOutput.getState());
            rcState.setId(rcOutput.getId());
            rcOutputStates.add(rcState);
        }
        rcUpdateIODTO.setOutputs(rcOutputStates);
        List<RCUpdateIODTO.RCState> rcInputStates = new ArrayList<>();
        for (RCInput rcInput : relayController.getInputs()) {
            RCUpdateIODTO.RCState rcState = new RCUpdateIODTO.RCState();
            rcState.setState(rcInput.getState());
            rcState.setId(rcInput.getId());
            rcInputStates.add(rcState);
        }
        rcUpdateIODTO.setInputs(rcInputStates);
        return rcUpdateIODTO;
    }
}

