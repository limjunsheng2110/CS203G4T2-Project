package com.cs203.tariffg4t2.repository.chatbot;

import com.cs203.tariffg4t2.model.chatbot.HsChatSessionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HsChatSessionLogRepository extends JpaRepository<HsChatSessionLog, String> {
}

