package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.entity.Controller;
import kz.home.RelaySmartSystems.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ControllerRepository extends JpaRepository<Controller, UUID> {
    Controller findByMac(String mac);
    List<Controller> findByUser(User user);

    @Query(value = "update controllers " +
                   "   set status = 'offline' " +
                   " where status = 'online' " +
                   "   and coalesce(last_seen, now()-interval '5 minute') < now()-interval '2 minute';", nativeQuery = true)
    //@Query(value = "select setOffline()", nativeQuery = true) // отправляет результат, хотя void
    @Modifying
    @Transactional
    void setOffline();
}


