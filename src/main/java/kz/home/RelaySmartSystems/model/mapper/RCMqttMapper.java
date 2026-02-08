package kz.home.RelaySmartSystems.model.mapper;

import kz.home.RelaySmartSystems.model.dto.RCMqttDTO;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCMqtt;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCMqttEvent;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCMqttTopic;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RCMqttMapper {
    public RCMqtt toEntity(RCMqttDTO rcMqttDTO) {
        if (rcMqttDTO == null)
            return null;
        RCMqtt rcMqtt = new RCMqtt();
        rcMqtt.setUrl(rcMqttDTO.getUrl());
        rcMqtt.setEnabled(rcMqtt.isEnabled());

        List<RCMqttTopic> rcMqttTopics = new ArrayList<>();
        for (RCMqttDTO.RCMqttTopicDTO rcMqttTopicDTO : rcMqttDTO.getTopics()) {
            RCMqttTopic rcMqttTopic = new RCMqttTopic();
            rcMqttTopic.setMqtt(rcMqtt);
            rcMqttTopic.setTopic(rcMqttTopicDTO.getTopic());

            List<RCMqttEvent> rcMqttEvents = new ArrayList<>();
            for (RCMqttDTO.RCMqttEventDTO rcMqttEventDTO : rcMqttTopicDTO.getEvents()) {
                RCMqttEvent rcMqttEvent = new RCMqttEvent();
                rcMqttEvent.setTopic(rcMqttTopic);
                rcMqttEvent.setEvent(rcMqttEventDTO.getEvent());
                rcMqttEvent.setType(rcMqttEventDTO.getType());
                rcMqttEvent.setOutput(rcMqttEventDTO.getOutput());
                rcMqttEvent.setInput(rcMqttEventDTO.getInput());
                rcMqttEvent.setAction(rcMqttEventDTO.getAction());
                rcMqttEvent.setSlaveId(rcMqttEventDTO.getSlaveId());
                rcMqttEvents.add(rcMqttEvent);
            }
            rcMqttTopic.setEvents(rcMqttEvents);
            rcMqttTopics.add(rcMqttTopic);
        }
        rcMqtt.setTopics(rcMqttTopics);

        return rcMqtt;
    }

    public RCMqttDTO toDto(RCMqtt rcMqtt) {
        if (rcMqtt == null)
            return null;
        RCMqttDTO dto = new RCMqttDTO();
        dto.setUrl(rcMqtt.getUrl());
        dto.setEnabled(rcMqtt.isEnabled());

        List<RCMqttDTO.RCMqttTopicDTO> topicDTOs = new ArrayList<>();
        if (rcMqtt.getTopics() != null) {
            for (RCMqttTopic topic : rcMqtt.getTopics()) {
                RCMqttDTO.RCMqttTopicDTO topicDTO = new RCMqttDTO.RCMqttTopicDTO();
                topicDTO.setTopic(topic.getTopic());

                List<RCMqttDTO.RCMqttEventDTO> eventDTOs = new ArrayList<>();
                if (topic.getEvents() != null) {
                    for (RCMqttEvent event : topic.getEvents()) {
                        RCMqttDTO.RCMqttEventDTO eventDTO = new RCMqttDTO.RCMqttEventDTO();
                        eventDTO.setEvent(event.getEvent());
                        eventDTO.setType(event.getType());
                        eventDTO.setOutput(event.getOutput());
                        eventDTO.setInput(event.getInput());
                        eventDTO.setAction(event.getAction());
                        eventDTO.setSlaveId(event.getSlaveId());
                        eventDTOs.add(eventDTO);
                    }
                }
                topicDTO.setEvents(eventDTOs);
                topicDTOs.add(topicDTO);
            }
        }
        dto.setTopics(topicDTOs);

        return dto;
    }

}
