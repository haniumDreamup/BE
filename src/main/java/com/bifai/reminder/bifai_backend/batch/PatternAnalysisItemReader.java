package com.bifai.reminder.bifai_backend.batch;

import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 패턴 분석을 위한 사용자 ItemReader
 * 활성 사용자를 페이징하여 읽어옴
 */
@Slf4j
public class PatternAnalysisItemReader implements ItemReader<User> {
  
  @Autowired
  private UserRepository userRepository;
  
  @Setter
  private String analysisDate;
  
  private List<User> users = new ArrayList<>();
  private int currentIndex = 0;
  private int currentPage = 0;
  private final int pageSize = 100;
  private boolean lastPage = false;
  
  @Override
  public User read() throws Exception {
    if (users.isEmpty() || currentIndex >= users.size()) {
      if (lastPage) {
        return null; // 모든 데이터 읽기 완료
      }
      fetchNextPage();
    }
    
    if (currentIndex < users.size()) {
      User user = users.get(currentIndex);
      currentIndex++;
      log.debug("Reading user: {} for pattern analysis", user.getUserId());
      return user;
    }
    
    return null;
  }
  
  private void fetchNextPage() {
    Pageable pageable = PageRequest.of(currentPage, pageSize);
    
    // 최근 7일 이내 활동한 사용자만 조회
    LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
    Page<User> page = userRepository.findActiveUsersAfter(sevenDaysAgo, pageable);
    
    if (page.hasContent()) {
      users = new ArrayList<>(page.getContent());
      currentIndex = 0;
      currentPage++;
      lastPage = page.isLast();
      log.info("Fetched {} users for analysis, page {}", users.size(), currentPage);
    } else {
      lastPage = true;
    }
  }
}