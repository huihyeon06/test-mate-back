package com.testmateback.dTestmate.controller;

import com.testmateback.dTestmate.dao.*;
import com.testmateback.dTestmate.dto.*;
import com.testmateback.dTestmate.entity.*;
import com.testmateback.dTestmate.repository.*;
import com.testmateback.dTestmate.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DMakerController {
    private final UserService userService;
    private final HomeService homeService;
    private final CalendarService calendarService;
    private final GoalService goalService;
    private final EditSubjectService editSubjectService;
    private final TestInfoService testInfoService;
    private final WrongNoteService wrongNoteService;


    @PostMapping("/sign-up")
    public CreateUser.Response createUser(
            @Valid @RequestBody CreateUser.Request request
    ) {
        log.info("request : {}", request);
        return userService.createUser(request);
    }

    @RestController
    @RequestMapping("/api")
    public class AuthController {

        @Autowired
        private UserRepository userRepository;

        @PostMapping("/login")
        public ResponseEntity<Map<String, String>> login(@RequestBody CreateUser.Request credentials) {
            String email = credentials.getEmail();
            String password = credentials.getPassword();

            // 이메일로 사용자 찾기
            Users user = userRepository.findByEmail(email);
            if (user == null || !user.getPassword().equals(password)) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "로그인 실패");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 로그인 성공
            Map<String, String> response = new HashMap<>();
            response.put("message", "로그인 성공");
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/home")
    public CreateHome.Response createHome(
            @Valid @RequestBody CreateHome.Request request
    ) {
        log.info("request : {}", request);
        return homeService.createHome(request);

    }

    @PostMapping("/calendar")
    public CreateCalendar.Response createCalendar(
            @Valid @RequestBody CreateCalendar.Request request
    ) {
        log.info("request : {}", request);
        return calendarService.createCalendar(request);

    }

    @PostMapping("/edit-subject")
    public CreateEditSubject.Response createEditSubject(
            @Valid @RequestBody CreateEditSubject.Request request
    ) {
        log.info("request : {}", request);
        return editSubjectService.createEditSubject(request);
    }

    @PostMapping("/goal")
    public CreateGoal.Response createGoal(
            @Valid @RequestBody CreateGoal.Request request
    ) {
        log.info("request : {} ", request);
        return goalService.createGoal(request);
    }

    @PostMapping("/test-info")
    public CreateTestInfo.Response createTestInfo(
            @Valid @RequestBody CreateTestInfo.Request request
    ) {
        log.info("request : {} ", request);
        return testInfoService.createTestInfo(request);
    }

    @PostMapping("/wrong-note")
    public CreateWrongNote.Response createWrongNote(
            @Valid @RequestBody CreateWrongNote.Request request
    ) {
        log.info("request : {} ", request);
        return wrongNoteService.createWrongNote(request);
    }

    @RestController
    @RequestMapping("/goal")
    public class GoalController {

        @Autowired
        private GoalRepository goalRepository;

        @Autowired
        private TestInfoRepository testInfoRepository;
        @Autowired
        private EditSubjectRepository editSubjectRepository; // EditSubject 테이블에 접근하기 위한 리포지토리 추가


        @GetMapping("/details")
        public List<GoalDetails> getGoalDetails(@RequestParam String indexes) {
            List<GoalDetails> goalDetailsList = new ArrayList<>();

            // testInfoRepository 인스턴스를 사용하여 findByIndexes 메서드 호출
            List<TestInfo> testInfoList = testInfoRepository.findByIndexes(indexes);

            EditSubject editSubject = editSubjectRepository.findBySubject(indexes);

            for (TestInfo testInfo : testInfoList) {
                GoalDetails goalDetails = new GoalDetails();
                goalDetails.setIndexes(indexes);
                goalDetails.setGoalSubject(editSubject.getSubject());
                goalDetails.setGoalGrade(testInfo.getGrade());

                // EditSubject 테이블에서 subject에 해당하는 photo를 가져옵니다.
                if (editSubject != null) {
                    goalDetails.setSubjectImg(editSubject.getPhoto());
                }

                // goalRepository를 사용하여 해당 subject의 goal 개수를 구합니다.
                List<Goal> goalsBySubject = goalRepository.findBySubject(editSubject.getSubject());
                List<Goal> checkedGoalsBySubject = goalRepository.findBySubjectAndChecksTrue(editSubject.getSubject());

                goalDetails.setTotalGoals(goalsBySubject.size());
                goalDetails.setCheckedGoals(checkedGoalsBySubject.size());
                goalDetailsList.add(goalDetails);
            }

            return goalDetailsList;
        }


        @GetMapping("/check-lists")
        public GoalCheckResponse getGoalCheck(
                @RequestParam String indexes,
                @RequestParam String subject,
                @RequestParam String grade
        ) {
            GoalCheckResponse response = new GoalCheckResponse();

            // Checked Goals 조회 (checks가 1인 것)
            List<Goal> checkedGoals = goalRepository.findByIndexesAndSubjectAndGradeAndChecks(indexes, subject, grade, true);
            List<String> checkedGoalStrings = new ArrayList<>();
            for (Goal goal : checkedGoals) {
                checkedGoalStrings.add(goal.getGoal());
            }
            response.setCheckedGoals(checkedGoalStrings);

            // No Goals 조회 (checks가 0인 것)
            List<Goal> noGoals = goalRepository.findByIndexesAndSubjectAndGradeAndChecks(indexes, subject, grade, false);
            List<String> noGoalStrings = new ArrayList<>();
            for (Goal goal : noGoals) {
                noGoalStrings.add(goal.getGoal());
            }
            response.setNoGoals(noGoalStrings);

            return response;
        }
    }

    @RestController
    @RequestMapping("/home")
    public class HomeController {

        @Autowired
        private EditSubjectRepository editSubjectRepository;

        @Autowired
        private TestInfoRepository testInfoRepository;

        @Autowired
        private WrongNoteRepository wrongNoteRepository;

        @Autowired
        private HomeRepository homeRepository;

        @GetMapping("/get-home-info")
        public HomeInfoResponse getHomeInfo(
                @RequestParam String indexes,
                @RequestParam String subject,
                @RequestParam String grade
        ) {
            HomeInfoResponse response = new HomeInfoResponse();

            // EditSubject에서 indexes와 subject에 해당하는 데이터를 가져와서 리스트로 만듭니다.
            List<EditSubject> editSubjects = editSubjectRepository.findByIndexesAndSubjectAndGrade(indexes, subject, grade);

            List<EditSubjectData> editSubjectDataList = new ArrayList<>();
            for (EditSubject editSubject : editSubjects) {
                EditSubjectData data = new EditSubjectData();
                data.setSubject(editSubject.getSubject());
                data.setPhoto(editSubject.getPhoto());
                editSubjectDataList.add(data);
            }
            response.setEditSubjectList(editSubjectDataList);

            // TestInfo에서 indexes와 subject에 해당하는 데이터를 가져와서 리스트로 만듭니다.
            List<TestInfo> testInfos = testInfoRepository.findByIndexesAndSubject(indexes, subject);

            List<TestInfoData> testInfoDataList = new ArrayList<>();
            for (TestInfo testInfo : testInfos) {
                TestInfoData data = new TestInfoData();
                data.setSubject(testInfo.getSubject());
                data.setgrade(testInfo.getGrade());
                data.setScore(testInfo.getScore());
                data.setDates(testInfo.getDates());
                data.setLevels(testInfo.getLevels());
                data.setTarget(testInfo.getTarget());
                testInfoDataList.add(data);
            }
            response.setTestInfoList(testInfoDataList);

            // WrongNote에서 indexes, grade, subject에 해당하는 데이터를 가져와서 처리합니다.
            List<WrongNote> wrongNotes = wrongNoteRepository.findByIndexesAndGradeAndSubject(indexes, grade, subject);

            List<WrongNoteData> wrongNoteDataList = new ArrayList<>();
            for (WrongNote wrongNote : wrongNotes) {
                WrongNoteData data = new WrongNoteData();
                // WrongNote 엔터티에 있는 필드들을 가져와서 data 객체에 설정해주세요.
                data.setReasoncheck(wrongNote.getReasoncheck());

                data.setReason(wrongNote.getReason());

                wrongNoteDataList.add(data);
            }
            response.setWrongNoteList(wrongNoteDataList);

            // Home에서 indexes, grade, subject가 fail인 데이터를 가져와서 처리합니다.
            List<Home> homeData = homeRepository.findByIndexesAndGradeAndSubject(indexes, grade, subject);

            List<HomeData> homeDataList = new ArrayList<>();
            for (Home home : homeData) {
                HomeData data = new HomeData();
                // Home 엔터티에 있는 필드들을 가져와서 data 객체에 설정해주세요.
                data.setFail(home.getFail());

                homeDataList.add(data);
            }
            response.setHomeList(homeDataList);

            return response;
        }
    }

}
