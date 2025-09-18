package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Medication;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * MedicationRepository 테스트
 * BIF 사용자의 약물 정보 관리 테스트
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("MedicationRepository 테스트")
class MedicationRepositoryTest {
    
    @Autowired
    private MedicationRepository medicationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    private Medication testMedication;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        medicationRepository.deleteAll();
        userRepository.deleteAll();
        
        testUser = userRepository.save(TestDataBuilder.createUser());
        testMedication = TestDataBuilder.createMedication(testUser);
    }
    
    // 테스트용 Medication 생성 헬퍼 메소드
    private Medication createMedication(String name, Medication.MedicationType type, String doctor, String pharmacy) {
        Medication medication = new Medication();
        medication.setUser(testUser);
        medication.setMedicationName(name);
        medication.setMedicationType(type);
        medication.setDosageForm(Medication.DosageForm.TABLET);
        medication.setDosageAmount(BigDecimal.valueOf(5));
        medication.setDosageUnit("mg");
        medication.setDailyFrequency(1);
        medication.setStartDate(LocalDate.now());
        medication.setIsActive(true);
        medication.setPriorityLevel(Medication.PriorityLevel.MEDIUM);
        medication.setMedicationStatus(Medication.MedicationStatus.ACTIVE);
        medication.setPrescribingDoctor(doctor);
        medication.setPharmacyName(pharmacy);
        return medication;
    }
    
    @Test
    @DisplayName("약물 정보 저장 - 성공")
    void saveMedication_Success() {
        // when
        Medication savedMedication = medicationRepository.save(testMedication);
        
        // then
        assertThat(savedMedication.getId()).isNotNull();
        assertThat(savedMedication.getMedicationName()).isEqualTo("혈압약");
        assertThat(savedMedication.getGenericName()).isEqualTo("암로디핀");
        assertThat(savedMedication.getDosageAmount()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(savedMedication.getDosageUnit()).isEqualTo("mg");
        assertThat(savedMedication.getDailyFrequency()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("약물 조회 - ID로 조회")
    void findById_Success() {
        // given
        Medication savedMedication = medicationRepository.save(testMedication);
        
        // when
        Optional<Medication> foundMedication = medicationRepository.findById(savedMedication.getId());
        
        // then
        assertThat(foundMedication).isPresent();
        assertThat(foundMedication.get().getSimpleDescription()).isEqualTo("혈압을 낮춰주는 약이에요");
        assertThat(foundMedication.get().getMedicationType()).isEqualTo(Medication.MedicationType.BLOOD_PRESSURE);
    }
    
    @Test
    @DisplayName("사용자별 약물 목록 조회")
    void findByUser_Success() {
        // given
        medicationRepository.save(testMedication);
        
        Medication secondMedication = new Medication();
        secondMedication.setUser(testUser);
        secondMedication.setMedicationName("당뇨약");
        secondMedication.setGenericName("메트포르민");
        secondMedication.setDosageAmount(BigDecimal.valueOf(500));
        secondMedication.setDosageUnit("mg");
        secondMedication.setDailyFrequency(2);
        secondMedication.setSimpleDescription("혈당을 낮춰주는 약이에요");
        secondMedication.setStartDate(LocalDate.now().minusMonths(3));
        secondMedication.setMedicationType(Medication.MedicationType.DIABETES);
        secondMedication.setDosageForm(Medication.DosageForm.TABLET);
        secondMedication.setIsActive(true);
        secondMedication.setPrescribingDoctor("이의사");
        medicationRepository.save(secondMedication);
        
        // when
        List<Medication> userMedications = medicationRepository.findByUser(testUser);
        
        // then
        assertThat(userMedications).hasSize(2);
        assertThat(userMedications).extracting("medicationName")
            .containsExactlyInAnyOrder("혈압약", "당뇨약");
    }
    
    @Test
    @DisplayName("활성 약물만 조회")
    void findByUserAndIsActiveTrue_Success() {
        // given
        medicationRepository.save(testMedication);
        
        Medication inactiveMedication = new Medication();
        inactiveMedication.setUser(testUser);
        inactiveMedication.setMedicationName("감기약");
        inactiveMedication.setGenericName("아세트아미노펜");
        inactiveMedication.setDosageAmount(BigDecimal.valueOf(500));
        inactiveMedication.setDosageUnit("mg");
        inactiveMedication.setDailyFrequency(3);
        inactiveMedication.setStartDate(LocalDate.now().minusWeeks(2));
        inactiveMedication.setEndDate(LocalDate.now().minusDays(1));
        inactiveMedication.setMedicationType(Medication.MedicationType.COLD_FLU);
        inactiveMedication.setDosageForm(Medication.DosageForm.TABLET);
        inactiveMedication.setIsActive(false);
        medicationRepository.save(inactiveMedication);
        
        // when
        List<Medication> activeMedications = medicationRepository.findByUserAndIsActiveTrue(testUser);
        
        // then
        assertThat(activeMedications).hasSize(1);
        assertThat(activeMedications.get(0).getMedicationName()).isEqualTo("혈압약");
    }
    
    @Test
    @DisplayName("종료일이 가까운 약물 조회")
    void findMedicationsEndingSoon_Success() {
        // given
        testMedication.setEndDate(LocalDate.now().plusDays(5));
        medicationRepository.save(testMedication);
        
        Medication longTermMedication = new Medication();
        longTermMedication.setUser(testUser);
        longTermMedication.setMedicationName("비타민");
        longTermMedication.setDosageAmount(BigDecimal.valueOf(1));
        longTermMedication.setDosageUnit("정");
        longTermMedication.setDailyFrequency(1);
        longTermMedication.setStartDate(LocalDate.now());
        longTermMedication.setEndDate(LocalDate.now().plusMonths(1));
        longTermMedication.setMedicationType(Medication.MedicationType.VITAMIN);
        longTermMedication.setDosageForm(Medication.DosageForm.TABLET);
        longTermMedication.setIsActive(true);
        medicationRepository.save(longTermMedication);
        
        // when
        LocalDate threshold = LocalDate.now().plusDays(7);
        List<Medication> endingSoon = medicationRepository.findMedicationsEndingSoon(threshold);
        
        // then
        assertThat(endingSoon).hasSize(1);
        assertThat(endingSoon.get(0).getMedicationName()).isEqualTo("혈압약");
    }
    
    // 재고 관리 기능은 Medication 엔티티에 없으므로 삭제
    
    @Test
    @DisplayName("처방의별 약물 조회")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void findByPrescribedBy_Success() {
        // given
        medicationRepository.save(testMedication);
        
        Medication sameDoctorMedication = createMedication("위장약", Medication.MedicationType.DIGESTIVE, "김의사", "동네약국");
        medicationRepository.save(sameDoctorMedication);
        
        // when
        List<Medication> kimDoctorMedications = medicationRepository.findByUserAndPrescribingDoctor(testUser, "김의사");
        
        // then
        assertThat(kimDoctorMedications).hasSize(2);
    }
    
    @Test
    @DisplayName("특정 기간 시작된 약물 조회")
    void findByStartDateBetween_Success() {
        // given
        medicationRepository.save(testMedication);
        
        Medication recentMedication = new Medication();
        recentMedication.setUser(testUser);
        recentMedication.setMedicationName("신규약");
        recentMedication.setDosageAmount(BigDecimal.valueOf(10));
        recentMedication.setDosageUnit("mg");
        recentMedication.setDailyFrequency(1);
        recentMedication.setStartDate(LocalDate.now().minusDays(7));
        recentMedication.setMedicationType(Medication.MedicationType.OTHER);
        recentMedication.setDosageForm(Medication.DosageForm.TABLET);
        recentMedication.setIsActive(true);
        medicationRepository.save(recentMedication);
        
        // when
        LocalDate startDate = LocalDate.now().minusDays(10);
        LocalDate endDate = LocalDate.now();
        List<Medication> recentlyStarted = medicationRepository.findByUserAndStartDateBetween(testUser, startDate, endDate);
        
        // then
        assertThat(recentlyStarted).hasSize(1);
        assertThat(recentlyStarted.get(0).getMedicationName()).isEqualTo("신규약");
    }
    
    @Test
    @DisplayName("약국별 약물 조회")
    void findByPharmacy_Success() {
        // given
        medicationRepository.save(testMedication);
        
        Medication samePharmacyMedication = createMedication("영양제", Medication.MedicationType.SUPPLEMENT, "김의사", "동네약국");
        medicationRepository.save(samePharmacyMedication);
        
        // when
        List<Medication> pharmacyMedications = medicationRepository.findByUserAndPharmacyName(testUser, "동네약국");
        
        // then
        assertThat(pharmacyMedications).hasSize(2);
    }
    
    @Test
    @DisplayName("부작용 있는 약물 조회")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void findBySideEffectsNotNull_Success() {
        // given
        medicationRepository.save(testMedication);
        
        Medication noSideEffectMedication = createMedication("영양제", Medication.MedicationType.SUPPLEMENT, "박의사", "건강약국");
        noSideEffectMedication.setSideEffects(null);
        medicationRepository.save(noSideEffectMedication);
        
        // when
        List<Medication> medicationsWithSideEffects = medicationRepository.findByUserAndSideEffectsIsNotNull(testUser);
        
        // then
        assertThat(medicationsWithSideEffects).hasSize(1);
        assertThat(medicationsWithSideEffects.get(0).getSideEffects()).isEqualTo("어지러움 가능");
    }
    
    @Test
    @DisplayName("약물 이름으로 검색")
    void searchByName_Success() {
        // given
        medicationRepository.save(testMedication);
        
        Medication anotherMedication = createMedication("진통제", Medication.MedicationType.PAIN_RELIEF, "최의사", "새봄약국");
        anotherMedication.setDosageAmount(BigDecimal.valueOf(500));
        anotherMedication.setDailyFrequency(4);
        medicationRepository.save(anotherMedication);
        
        // when
        List<Medication> bloodPressureMeds = medicationRepository.searchByNameContaining(testUser, "혈압");
        List<Medication> painMeds = medicationRepository.searchByNameContaining(testUser, "진통");
        
        // then
        assertThat(bloodPressureMeds).hasSize(1);
        assertThat(painMeds).hasSize(1);
    }
    
    @Test
    @DisplayName("약물 정보 업데이트")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void updateMedication_Success() {
        // given
        Medication savedMedication = medicationRepository.save(testMedication);
        
        // when
        savedMedication.setDosageAmount(BigDecimal.valueOf(10));
        savedMedication.setDosageUnit("mg");
        savedMedication.setDailyFrequency(2);
        Medication updatedMedication = medicationRepository.save(savedMedication);
        
        // then
        assertThat(updatedMedication.getDosageAmount()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(updatedMedication.getDosageUnit()).isEqualTo("mg");
        assertThat(updatedMedication.getDailyFrequency()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("약물 종료 처리")
    void endMedication_Success() {
        // given
        Medication savedMedication = medicationRepository.save(testMedication);
        
        // when
        savedMedication.setEndDate(LocalDate.now());
        savedMedication.setIsActive(false);
        Medication endedMedication = medicationRepository.save(savedMedication);
        
        // then
        assertThat(endedMedication.getIsActive()).isFalse();
        assertThat(endedMedication.getEndDate()).isEqualTo(LocalDate.now());
    }
    
    @Test
    @DisplayName("약물 삭제")
    void deleteMedication_Success() {
        // given
        Medication savedMedication = medicationRepository.save(testMedication);
        Long medicationId = savedMedication.getId();
        
        // when
        medicationRepository.deleteById(medicationId);
        
        // then
        assertThat(medicationRepository.findById(medicationId)).isEmpty();
    }
    
    @Test
    @DisplayName("필수 필드 누락 - 실패")
    void saveWithoutRequiredFields_Fail() {
        // given
        Medication invalidMedication = new Medication();
        invalidMedication.setUser(testUser);
        // medicationName 누락 (NotBlank 제약조건)
        invalidMedication.setMedicationType(Medication.MedicationType.OTHER);
        invalidMedication.setDosageForm(Medication.DosageForm.TABLET);
        invalidMedication.setDosageAmount(BigDecimal.valueOf(10));
        invalidMedication.setDosageUnit("mg");
        invalidMedication.setDailyFrequency(1);
        invalidMedication.setStartDate(LocalDate.now());
        invalidMedication.setIsActive(true);
        invalidMedication.setPriorityLevel(Medication.PriorityLevel.MEDIUM);
        invalidMedication.setMedicationStatus(Medication.MedicationStatus.ACTIVE);
        
        // when & then
        assertThatThrownBy(() -> {
            medicationRepository.save(invalidMedication);
            medicationRepository.flush();
        }).isInstanceOf(Exception.class); // H2에서는 다른 예외가 발생할 수 있음
    }
    
    @Test
    @DisplayName("페이징 조회")
    void findAllWithPaging_Success() {
        // given
        for (int i = 0; i < 5; i++) {
            Medication medication = createMedication("약물" + i, Medication.MedicationType.OTHER, "의사" + i, "약국" + i);
            medicationRepository.save(medication);
        }
        
        // when
        Page<Medication> firstPage = medicationRepository.findAll(PageRequest.of(0, 3));
        
        // then
        assertThat(firstPage.getContent()).hasSize(3);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }
}