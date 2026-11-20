package ro.oks.quickjob.user;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.oks.quickjob.common.error.BusinessException;
import ro.oks.quickjob.common.error.ErrorCode;
import ro.oks.quickjob.common.geo.GeoUtils;
import ro.oks.quickjob.mission.MissionCategory;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.USER_NOT_FOUND,
                        "user.not_found",
                        HttpStatus.NOT_FOUND));
    }

    public User getByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.USER_NOT_FOUND,
                        "user.not_found",
                        HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void updateLocation(UUID userId, double latitude, double longitude) {
        User user = getById(userId);
        Point location = GeoUtils.point(latitude, longitude);
        user.updateLocation(location);
    }

    @Transactional
    public void updateCategories(UUID userId, Set<MissionCategory> categories) {
        User user = getById(userId);
        user.updateCategories(categories);
    }

    @Transactional
    public void updateLanguage(UUID userId, String language) {
        User user = getById(userId);
        user.setPreferredLanguage(language);
    }

    public List<User> findNearbyWorkers(Point location, double radiusMeters,
                                         UUID excludeUserId) {
        return userRepository.findNearbyWorkers(location, radiusMeters, excludeUserId);
    }
}
