package com.qhoto.qhoto_api.api.service;

import com.qhoto.qhoto_api.api.repository.activequest.ActiveDailyRepository;
import com.qhoto.qhoto_api.api.repository.activequest.ActiveMonthlyRepository;
import com.qhoto.qhoto_api.api.repository.activequest.ActiveWeeklyRepository;
import com.qhoto.qhoto_api.api.repository.exp.ExpGradeRepository;
import com.qhoto.qhoto_api.api.repository.exp.ExpRepository;
import com.qhoto.qhoto_api.api.repository.feed.*;
import com.qhoto.qhoto_api.api.repository.quest.QuestRepository;
import com.qhoto.qhoto_api.api.repository.user.UserRepository;
import com.qhoto.qhoto_api.domain.*;
import com.qhoto.qhoto_api.domain.type.CommentStatus;
import com.qhoto.qhoto_api.domain.type.FeedLikePK;
import com.qhoto.qhoto_api.domain.type.FeedStatus;
import com.qhoto.qhoto_api.domain.type.FeedType;
import com.qhoto.qhoto_api.dto.request.*;
import com.qhoto.qhoto_api.dto.response.feed.CommentRes;
import com.qhoto.qhoto_api.dto.response.feed.FeedAllDto;
import com.qhoto.qhoto_api.dto.response.feed.FeedDetailRes;
import com.qhoto.qhoto_api.dto.response.feed.FeedFriendDto;
import com.qhoto.qhoto_api.dto.response.quest.QuestOptionItemRes;
import com.qhoto.qhoto_api.dto.response.quest.QuestOptionRes;
import com.qhoto.qhoto_api.dto.type.LikeStatus;
import com.qhoto.qhoto_api.exception.NoFeedByIdException;
import com.qhoto.qhoto_api.exception.NoQuestByIdException;
import com.qhoto.qhoto_api.exception.NoUserByIdException;
import com.qhoto.qhoto_api.util.S3Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class FeedService {


    private final S3Utils s3Utils;
    private final CommentRepository commentRepository;
    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final QuestRepository questRepository;
    private final ActiveDailyRepository activeDailyRepository;
    private final ExpGradeRepository expGradeRepository;
    private final ActiveWeeklyRepository activeWeeklyRepository;
    private final ActiveMonthlyRepository activeMonthlyRepository;
    private final ExpRepository expRepository;



    // ?????? ?????? ????????????
    public Page<FeedAllDto> getFeed(User user, FeedAllReq feedAllReq, Pageable pageable) {
        return feedRepository.findByCondition(user,feedAllReq, pageable);
    }

    // ?????? ?????? ?????? ????????????
    public FeedDetailRes getFeedDetail(Long feedId, User userInfo, Pageable pageable){

        // ?????? ?????? ??????
        Feed feed = feedRepository.findFeedById(feedId).orElseThrow(() -> new NoFeedByIdException("no feed by id"));
        // ?????? ?????? ??????
        User user = userRepository.findUserById(feed.getUser().getId()).orElseThrow(()-> new NoUserByIdException("no user by id"));
        // ??????????????? ????????????
        Page<CommentRes> commentResList = getCommentList(feedId, pageable);
        // ?????? ?????? ?????? ??????
        FeedDetailRes feedDetailRes = FeedDetailRes.builder()
                .feedId(feedId)
                .userId(user.getId())
                .userImage(user.getImage())
                .nickname(user.getNickname())
                .feedImage(feed.getImage())
                .feedTime(feed.getTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd a hh:mm").localizedBy(Locale.KOREA)))
                .questName(feed.getQuest().getName())
                .questType(feed.getQuest().getQuestType().getCode())
                .questPoint(feed.getQuest().getScore())
                .expGrade(user.getExpGrade())
                .expPoint(user.getTotalExp())
                .likeCount(feedLikeRepository.countAllById(feedId).orElseThrow(()-> new NoFeedByIdException("no feed by id")))
                .likeStatus((feedLikeRepository.findById(userInfo.getId(),feed.getId()).isPresent())?LikeStatus.LIKE:LikeStatus.UNLIKE)
                .commentList(commentResList)
                .feedType(feed.getFeedType())
                .build();

        return feedDetailRes;
    }

    // ??????????????? ????????????
    private Page<CommentRes> getCommentList(Long feedId, Pageable pageable) {
        // ?????? ?????? ??????
        Page<Comment> commentList = commentRepository.findListById(feedId, pageable);
        Page<CommentRes> commentResList = commentList.map(comment->
                     CommentRes.builder()
                    .userId(comment.getUser().getId())
                    .nickname(comment.getUser().getNickname())
                    .userImage(comment.getUser().getImage())
                    .commentContext(comment.getContext())
                    .commentTime(comment.getTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd a hh:mm").localizedBy(Locale.KOREA)))
                    .build());
        return commentResList;

        // ??????????????? ??????
//        for (Comment comment : commentList) {
//            commentResList.add(CommentRes.builder()
//                    .userId(comment.getUser().getId())
//                    .nickname(comment.getUser().getNickname())
//                    .userImage(comment.getUser().getImage())
//                    .commentContext(comment.getContext())
//                    .commentTime(comment.getTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
//                    .build());
    }

    // ?????? ?????? ????????????
    public Page<FeedFriendDto> getFriendFeed(User user, FeedAllReq feedAllReq, Pageable pageable){
        return feedRepository.findByConditionAndUserId(feedAllReq, pageable, user.getId());
    }


    // ?????? ?????? ????????????
    public void postFeed(CreateFeedReq createFeedReq,User userInfo) throws IOException {
        // ????????? ?????? ????????????
        Quest quest = questRepository.findQuestById(createFeedReq.getQuestId()).orElseThrow(()-> new NoQuestByIdException("no quest by id"));
        // ?????? ?????? ????????????
        User user = userRepository.findUserById(userInfo.getId()).orElseThrow(()-> new NoUserByIdException("no user by id"));
        String dirName = "feed/image/"+user.getEmail();
        // S3??? upload ??????
        S3upload(createFeedReq, quest, user, dirName, FeedType.IMAGE);
    }

    // ????????? ?????? ????????????
    public void postVideoFeed(CreateFeedReq createFeedReq,User userInfo) throws IOException {
        // ????????? ?????? ????????????
        Quest quest = questRepository.findQuestById(createFeedReq.getQuestId()).orElseThrow(()-> new NoQuestByIdException("no quest by id"));;
        // ?????? ?????? ????????????
        User user = userRepository.findUserById(userInfo.getId()).orElseThrow(()-> new NoUserByIdException("no user by id"));
        String dirName = "feed/video/input/"+user.getEmail();
        // S3??? upload ??????
        S3upload(createFeedReq, quest, user, dirName, FeedType.VIDEO);

    }

    // S3??? upload ??????
    private void S3upload(CreateFeedReq createFeedReq, Quest quest, User user, String dirName, FeedType feedType) throws IOException {
        s3Utils.upload(createFeedReq.getFeedImage(),dirName);
        // DB?????? ???????????? ????????? ????????? ????????? ?????? ????????? ????????????
        dirName = S3Utils.CLOUD_FRONT_DOMAIN_NAME+"/"+dirName;
        // ?????? ??????
        Feed feed = Feed.builder()
                .user(user)
                .quest(quest)
                .activeDaily(activeDailyRepository.findDailyById(createFeedReq.getActiveDailyId()))
                .activeWeekly(activeWeeklyRepository.findWeeklyById(createFeedReq.getActiveWeeklyId()))
                .activeMonthly(activeMonthlyRepository.findMonthlyById(createFeedReq.getActiveMonthlyId()))
                .image(dirName+"/"+createFeedReq.getFeedImage().getOriginalFilename())
                .time(LocalDateTime.now())
                .status(FeedStatus.USING)
                .questName(quest.getName())
                .location(createFeedReq.getLocation())
                .typeCode(quest.getQuestType().getCode())
                .typeName(quest.getQuestType().getName())
                .score(quest.getScore())
                .difficulty(quest.getDifficulty())
                .duration(quest.getDuration())
                .feedType(feedType)
                .build();
        // ????????? ????????????
        Exp exp = expRepository.findAllByTypeCodeAndUserId(quest.getQuestType().getCode(),user.getId());
        // ????????? ??????
        exp.addPoint(quest.getScore());
        // ?????? ????????? ??????
        user.addTotalExp(exp.getPoint());
        PageRequest pageRequest = PageRequest.of(0,1);
        List<ExpGrade> expGrade = expGradeRepository.findByBoundaryPoint(user.getTotalExp(), pageRequest);
        user.updateGrade(expGrade.get(0).getGradeName());
        // ?????? ??????
        feedRepository.save(feed);
    }

    //?????? ??????
    public void deleteFeed(Long feedId) {
        feedRepository.deleteFeedByfeedId(feedId);
    }


    // ?????? ??????
    public void postComment(CreateCommentReq createCommentReq, User user){
        // ?????? ??????
        Comment comment = Comment.builder()
                .feed(feedRepository.findFeedById(createCommentReq.getFeedId()).orElseThrow(() -> new NoFeedByIdException("no feed by id")))
                .user(userRepository.findUserById(user.getId()).orElseThrow(()-> new NoUserByIdException("no user by id")))
                .context(createCommentReq.getCommentContext())
                .time(LocalDateTime.now())
                .status(CommentStatus.USING)
                .build();
        // ?????? ??????
        commentRepository.save(comment);
    }

    // ?????? ????????????
    public Page<CommentRes> getComment(Long feedId, Pageable pageable){
        return getCommentList(feedId,pageable);
    }

    // ?????? ????????????
    public void putComment(Long commentId){
        // ?????? ????????????
        Comment comment = commentRepository.findCommentById(commentId);
        // ?????? ?????? ????????????
        comment.changeCommentStatus(CommentStatus.DISABLE);
    }

    // ?????? ????????? ?????????
    public void postLike(LikeReq likeReq, User user){
        // ?????? ????????? ??????
        FeedLike feedLike = FeedLike.builder()
                .feed(feedRepository.findFeedById(likeReq.getFeedId()).orElseThrow(() -> new NoFeedByIdException("no feed by id")))
                .user(userRepository.findUserById(user.getId()).orElseThrow(()-> new NoUserByIdException("no user by id")))
                .build();
        // ?????? ????????? ??????
        feedLikeRepository.save(feedLike);
    }

    // ?????? ????????? ??????
    @Modifying
    public void deleteLike(User user, Long feedId){
        // ?????? ????????? ??????
        FeedLikePK feedLikePK = FeedLikePK.builder()
                .feed(feedRepository.findFeedById(feedId).orElseThrow(() -> new NoFeedByIdException("no feed by id")))
                .user(userRepository.findUserById(user.getId()).orElseThrow(()-> new NoUserByIdException("no user by id")))
                .build();
        // ?????? ????????? ??????
        feedLikeRepository.deleteById(feedLikePK);
    }

    public QuestOptionRes getQuestList() {

        // ????????? ?????? ????????? ????????? Map ??????
        Map<String, List<QuestOptionItemRes>> optionList = new HashMap<>();
        List<QuestOptionItemRes> dailyOptions = questRepository.findAllDailyByQuestIdAndStatus();
        List<QuestOptionItemRes> weeklyOptions = questRepository.findAllWeeklyByQuestIdAndStatus();
        List<QuestOptionItemRes> monthlyOptions = questRepository.findAllMonthlyByQuestIdAndStatus();
        optionList.put("dailyOptions", dailyOptions);
        optionList.put("weeklyOptions", weeklyOptions);
        optionList.put("monthlyOptions", monthlyOptions);

        // QuestOptionRes ??????
        QuestOptionRes QO = QuestOptionRes.builder()
                .options(optionList)
                .build();
        return QO;
    }

    public List<FeedDetailRes> getFeedListByTime(User user, DateReq date, Pageable pageable) {
        LocalDate requestDate = LocalDate.parse(date.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        List<Feed> feedList = feedRepository.findByTimeBetweenAndStatusAndUser(requestDate.atStartOfDay(), requestDate.atTime(LocalTime.MAX),FeedStatus.USING,user);
        return feedList.stream().map((feed) -> {
            Page<CommentRes> commentResList = getCommentList(feed.getId(),pageable);
            return FeedDetailRes.builder()
                    .feedId(feed.getId())
                    .userId(user.getId())
                    .userImage(user.getImage())
                    .nickname(user.getNickname())
                    .feedImage(feed.getImage())
                    .feedTime(feed.getTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd a hh:mm").localizedBy(Locale.KOREA)))
                    .duration(feed.getDuration())
                    .questName(feed.getQuest().getName())
                    .questType(feed.getQuest().getQuestType().getCode())
                    .questPoint(feed.getQuest().getScore())
                    .expGrade(user.getExpGrade())
                    .expPoint(user.getTotalExp())
                    .likeCount(feedLikeRepository.countAllById(feed.getId()).orElseThrow(() -> new NoFeedByIdException("no feed by id")))
                    .likeStatus((feedLikeRepository.findById(user.getId(), feed.getId()).isPresent()) ? LikeStatus.LIKE : LikeStatus.UNLIKE)
                    .commentList(commentResList)
                    .feedType(feed.getFeedType())
                    .build();
        }).collect(Collectors.toList());

    }
}
