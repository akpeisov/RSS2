package kz.home.RelaySmartSystems.model.mapper;

import kz.home.RelaySmartSystems.model.dto.RCSchedulerDTO;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RCSchedulerMapper {
    public RCScheduler toEntity(RCSchedulerDTO rcSchedulerDTO) {
        RCScheduler rcScheduler = new RCScheduler();
        if (rcSchedulerDTO == null)
            return null;
        rcScheduler.setEnabled(rcSchedulerDTO.isEnabled());

        List<RCTask> tasks = new ArrayList<>();
        for (RCSchedulerDTO.RCTaskDTO taskDTO : rcSchedulerDTO.getTasks()) {
            RCTask task = new RCTask();
            task.setScheduler(rcScheduler);
            task.setName(taskDTO.getName());
            task.setDone(taskDTO.isDone());
            task.setTime(taskDTO.getTime());
            task.setGrace(taskDTO.getGrace());
            task.setEnabled(taskDTO.isEnabled());
            task.setDow(taskDTO.getDow());
            List<RCTaskAction> rcTaskActions = new ArrayList<>();
            for (RCSchedulerDTO.RCTaskActionDTO rcTaskActionDTO : taskDTO.getActions()) {
                RCTaskAction rcTaskAction = new RCTaskAction();
                rcTaskAction.setTask(task);
                rcTaskAction.setType(rcTaskActionDTO.getType());
                rcTaskAction.setAction(rcTaskActionDTO.getAction());
                rcTaskAction.setOutput(rcTaskActionDTO.getOutput());
                rcTaskAction.setInput(rcTaskActionDTO.getInput());
                rcTaskActions.add(rcTaskAction);
            }
            task.setActions(rcTaskActions);
            tasks.add(task);
        }
        rcScheduler.setTasks(tasks);

        return rcScheduler;
    }

    public RCSchedulerDTO toDto(RCScheduler rcScheduler) {
        if (rcScheduler == null)
            return null;
        RCSchedulerDTO rcSchedulerDTO = new RCSchedulerDTO();
        rcSchedulerDTO.setEnabled(rcScheduler.isEnabled());

        List<RCSchedulerDTO.RCTaskDTO> tasksDtos = new ArrayList<>();
        for (RCTask task : rcScheduler.getTasks()) {
            RCSchedulerDTO.RCTaskDTO taskDTO = new RCSchedulerDTO.RCTaskDTO();
            taskDTO.setName(task.getName());
            taskDTO.setDone(task.isDone());
            taskDTO.setTime(task.getTime());
            taskDTO.setGrace(task.getGrace());
            taskDTO.setEnabled(task.isEnabled());
            taskDTO.setDow(task.getDow());
            List<RCSchedulerDTO.RCTaskActionDTO> rcTaskActionsDtos = new ArrayList<>();
            for (RCTaskAction rcTaskAction : task.getActions()) {
                RCSchedulerDTO.RCTaskActionDTO rcTaskActionDTO = new RCSchedulerDTO.RCTaskActionDTO();
                rcTaskActionDTO.setType(rcTaskAction.getType());
                rcTaskActionDTO.setAction(rcTaskAction.getAction());
                rcTaskActionDTO.setOutput(rcTaskAction.getOutput());
                rcTaskActionDTO.setInput(rcTaskAction.getInput());
                rcTaskActionsDtos.add(rcTaskActionDTO);
            }
            taskDTO.setActions(rcTaskActionsDtos);
            tasksDtos.add(taskDTO);
        }
        rcSchedulerDTO.setTasks(tasksDtos);

        return rcSchedulerDTO;
    }

}
