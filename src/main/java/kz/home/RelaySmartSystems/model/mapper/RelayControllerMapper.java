package kz.home.RelaySmartSystems.model.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.dto.*;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.*;
import org.springframework.stereotype.Service;

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

