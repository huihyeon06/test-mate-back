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
import java.util.stream.Collectors;

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

                // EditSubject가 null이 아닌 경우에만 값을 설정합니다.
                if (editSubject != null) {
                    goalDetails.setGoalSubject(editSubject.getSubject());
                    goalDetails.setSubjectImg(editSubject.getPhoto());
                }

                goalDetails.setGoalGrade(testInfo.getGrade());

                // goalRepository를 사용하여 해당 subject의 goal 개수를 구합니다.
                List<Goal> goalsBySubject = goalRepository.findBySubject(goalDetails.getGoalSubject());
                List<Goal> checkedGoalsBySubject = goalRepository.findBySubjectAndChecksTrue(goalDetails.getGoalSubject());

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
            try {
                List<Goal> checkedGoals = goalRepository.findByIndexesAndSubjectAndGradeAndChecks(indexes, subject, grade, true);
                List<Goal> noGoals = goalRepository.findByIndexesAndSubjectAndGradeAndChecks(indexes, subject, grade, false);

                GoalCheckResponse response = new GoalCheckResponse();
                response.setCheckedGoals(checkedGoals.stream().map(Goal::getGoal).collect(Collectors.toList()));
                response.setNoGoals(noGoals.stream().map(Goal::getGoal).collect(Collectors.toList()));

                return response;
            } catch (Exception e) {
                // 예외 처리: 예외가 발생할 경우 에러 응답을 반환하거나 로깅 등을 수행할 수 있습니다.
                e.printStackTrace();
                throw new RuntimeException("Failed to fetch goals");
            }
        }

        private final GoalRepository repository;

        @Autowired
        public GoalController(GoalRepository repository) {
            this.repository = repository;
        }

        @DeleteMapping("/{id}")
        public void deleteGoalById(@PathVariable Long id) {
            repository.deleteById(id);
        }

        @DeleteMapping("/delete-by-conditions")
        public void deleteGoalByConditions(
                @RequestParam String indexes,
                @RequestParam String subject,
                @RequestParam String grade,
                @RequestParam String goal) {

            repository.deleteByIndexesAndSubjectAndGradeAndGoal(indexes, subject, grade, goal);
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
    @RestController
    @RequestMapping("/wrong-note")
    public class wrongController {
        @Autowired
        private WrongNoteRepository wrongNoteRepository;
        @GetMapping("/details")
        public WrongNoteDetails getWrongNoteDetails(@RequestParam String indexes,@RequestParam String subject, @RequestParam String grade){
            WrongNoteDetails wrongNoteDetails = new WrongNoteDetails();
            wrongNoteDetails.setIndexes(indexes);

            WrongNote wrongNote = (WrongNote) wrongNoteRepository.findBySubjectAndGradeAndIndexes(indexes, grade, grade).orElse(null);

            if (wrongNote != null) {
                wrongNoteDetails.setWtitle(wrongNote.getTitle());
                wrongNoteDetails.setWreason(wrongNote.getReason());
                wrongNoteDetails.setWphoto(wrongNote.getPhoto());
                wrongNoteDetails.setWproblem(wrongNote.getProblem());
            }
            return wrongNoteDetails;
        }
    }

    @RestController
    @RequestMapping("/calendar")
    public class calendarController {
        @Autowired
        private CalenderRepository calenderRepository;
        @GetMapping("/details")
        public List<CalendarDetails> getCalendarDetails(@RequestParam String indexes) {
            // indexes에 해당하는 모든 결과를 가져옵니다.
            List<Calendar> calendars = calenderRepository.findByIndexes(indexes);

            List<CalendarDetails> calendarDetailsList = new ArrayList<>();

            for (Calendar calendar : calendars) {
                CalendarDetails calendarDetails = new CalendarDetails();
                calendarDetails.setSubjectdate(calendar.getDate());
                calendarDetails.setCsubject(calendar.getSubject());
                calendarDetailsList.add(calendarDetails);
            }

            return calendarDetailsList;
        }

    }
}
