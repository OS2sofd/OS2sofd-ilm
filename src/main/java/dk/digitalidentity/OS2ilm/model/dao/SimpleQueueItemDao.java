package dk.digitalidentity.OS2ilm.model.dao;

import dk.digitalidentity.simple_queue.entity.QueueItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimpleQueueItemDao extends JpaRepository<QueueItem, Long> {
    List<QueueItem> findAllByQueueAndStatus(String queue, QueueItem.QueueItemStatus status);
}
