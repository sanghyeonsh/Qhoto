package com.qhoto.qhoto_api.api.repository;

import com.qhoto.qhoto_api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>,UserRepositoryByCon {
    Optional<User> findByEmail(String email);
    Optional<User> findUserById(Long userId);

    Optional<User> findByRefreshToken(String refreshToken);
    boolean existsByEmail(String email);
    @Query("select u.refreshToken from User u where u.id = :id")
    String getRefreshTokenById(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query("update User u set u.refreshToken=:token where u.id=:id")
    void updateRefreshToken(@Param("id") Long id, @Param("token") String token);

    boolean existsByNickname(String nickname);

    @Query("select u from User u inner join fetch Friend f on u.id=f.followee.id where f.follower.id=:id")
    List<User> findFriendById(@Param("id") Long id);

    @Query("select u from User u inner join fetch FriendRequest r on r.requestUser.id=u.id where r.responseUser.id=:id and r.status='G'")
    List<User> findReceiveById(@Param("id") Long id);

}
