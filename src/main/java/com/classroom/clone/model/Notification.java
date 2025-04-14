package com.classroom.clone.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @ManyToOne
    private User recipient;

    private boolean read;

    // send(), markAsRead() etc.


}
