package john.api1.application.services.history;

import john.api1.application.components.enums.ActivityLogType;
import john.api1.application.components.exception.PersistenceException;
import john.api1.application.domain.cores.ActivityLogDS;
import john.api1.application.domain.cores.ActivityLogDataContext;
import john.api1.application.domain.models.ActivityLogDomain;
import john.api1.application.dto.mapper.history.ActivityLogDTO;
import john.api1.application.ports.repositories.history.IHistoryLogSearchRepository;
import john.api1.application.ports.repositories.pet.IPetSearchRepository;
import john.api1.application.ports.services.IPetOwnerSearch;
import john.api1.application.ports.services.boarding.IBoardingSearch;
import john.api1.application.ports.services.boarding.IPricingManagement;
import john.api1.application.ports.services.history.IHistoryLogSearch;
import john.api1.application.ports.services.pet.IPetSearch;
import john.api1.application.ports.services.request.IRequestSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Note: This layer throw persistence exception instead of handling. Expected other actors who use will catch gracefully!
@Service
public class HistoryLogSearchAS implements IHistoryLogSearch {
    private static final Logger log = LoggerFactory.getLogger(HistoryLogSearchAS.class);

    private final IHistoryLogSearchRepository searchRepository;
    private final IBoardingSearch boardingSearch;
    private final IPetOwnerSearch ownerSearch;
    private final IPetSearch petSearch;
    private final IPetSearchRepository petSearchRepository;
    private final IRequestSearch requestSearch;
    private final IPricingManagement pricingSearch;


    @Autowired
    public HistoryLogSearchAS(IHistoryLogSearchRepository searchRepository,
                              IBoardingSearch boardingSearch,
                              IPetOwnerSearch ownerSearch,
                              IPetSearch petSearch,
                              IPetSearchRepository petSearchRepository,
                              IRequestSearch requestSearch,
                              IPricingManagement pricingSearch) {
        this.searchRepository = searchRepository;
        this.boardingSearch = boardingSearch;
        this.ownerSearch = ownerSearch;
        this.petSearch = petSearch;
        this.petSearchRepository = petSearchRepository;
        this.requestSearch = requestSearch;
        this.pricingSearch = pricingSearch;
    }

    public Optional<ActivityLogDTO> getRecentLog() {
        try {
            var activity = searchRepository.searchRecently();
            if (activity.isEmpty()) throw new PersistenceException("No recent activity log found");

            ActivityLogDomain domain = activity.get();
            ActivityLogDataContext context = buildDataContext(domain);

            ActivityLogDTO dto = transformLog(domain, context);

            return Optional.ofNullable(dto);
        } catch (PersistenceException | NullPointerException e) {
            log.error("Error occurred while fetching activity logs: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public List<ActivityLogDTO> getAll() {
        try {
            List<ActivityLogDomain> activities = searchRepository.searchAll();
            if (activities.isEmpty()) {
                throw new PersistenceException("No activity logs found");
            }

            return activities.stream()
                    .map(domain -> {
                        ActivityLogDataContext context = buildDataContext(domain);
                        return transformLog(domain, context);
                    })
                    .collect(Collectors.toList());
        } catch (PersistenceException | NullPointerException e) {
            log.error("Error occurred while fetching activity logs: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Optional<ActivityLogDTO> searchById(String id) {

        try {
            Optional<ActivityLogDomain> activity = searchRepository.searchById(id);

            if (activity.isEmpty()) {
                throw new PersistenceException("Activity log not found with ID: " + id);
            }

            ActivityLogDomain domain = activity.get();
            ActivityLogDataContext context = buildDataContext(domain);
            return Optional.ofNullable(transformLog(domain, context));
        } catch (PersistenceException | NullPointerException e) {
            log.error("Error occurred while fetching activity logs: {}", e.getMessage());
            throw e;
        }
    }

    // LOW PRIORITY ///////////////
    public Optional<List<ActivityLogDTO>> getByDate(Instant time) {
        return Optional.empty();
    }

    public Optional<List<ActivityLogDTO>> getByBetweenDate(Instant start, Instant end) {
        return Optional.empty();
    }
    ////////////////////////////////

    @Override
    public List<ActivityLogDTO> searchByActivityType(ActivityLogType type) {
        try {
            List<ActivityLogDomain> activities = searchRepository.searchByActivityType(type);
            if (activities.isEmpty()) {
                return List.of();
            }

            return activities.stream()
                    .map(domain -> {
                        ActivityLogDataContext context = buildDataContext(domain);
                        if (context != null) {
                            return transformLog(domain, context);
                        }
                        return null;
                    })
                    .collect(Collectors.toList());
        } catch (PersistenceException | NullPointerException e) {
            log.error("Error occurred while fetching activity logs: {}", e.getMessage());
            throw e;
        }
    }

    private ActivityLogDataContext buildDataContext(ActivityLogDomain activity) {
        try {
            log.info("Before Switch: Starting searching data for " + activity.getActivityType().getActivityLogTypeToDTO());
            switch (activity.getActivityType()) {
                case BOARDING_MANAGEMENT -> {
                    log.info("Starting searching data for Boarding Management");
                    if (activity.getTypeId() != null) {
                        var boardingResult = boardingSearch.findBoardingById(activity.getTypeId());
                        if (boardingResult.isSuccess()) {
                            var boarding = boardingResult.getData();
                            var pricingOpt = pricingSearch.getBoardingPricingCqrs(boarding.getId());
                            var petOpt = petSearch.getPetBoardingDetails(boarding.getPetId());

                            if (pricingOpt.isPresent() && petOpt != null) {
                                log.info("Successfully build data for Boarding Management, proceed to transform data ");
                                return new ActivityLogDataContext.Builder()
                                        .boarding(boarding)
                                        .pricing(pricingOpt.get())
                                        .pet(petOpt)
                                        .build();
                            }
                        }
                    }
                    return null;
                }

                case PET_OWNER_MANAGEMENT -> {
                    log.info("Starting searching data for Pet Owner Management");
                    if (activity.getTypeId() != null) {

                        var ownerOpt = ownerSearch.getPetOwnerBoardingDetails(activity.getTypeId());
                        if (ownerOpt != null) {
                            log.info("Successfully build data for Pet Owner Management, proceed to transform data ");
                            return new ActivityLogDataContext.Builder()
                                    .owner(ownerOpt)
                                    .build();
                        }
                        return null;
                    }
                    return null;

                }

                // New Pet Created
                case PET_MANAGEMENT -> {
                    log.info("Starting searching data for Pet Management");
                    if (activity.getTypeId() != null) {

                        var pet = petSearchRepository.getPetById(activity.getTypeId());
                        if (pet.isPresent()) {
                            var ownerOpt = ownerSearch.getPetOwnerBoardingDetails(pet.get().getOwnerId());
                            var petOpt = petSearch.getPetBoardingDetails(pet.get().getId());

                            if (ownerOpt != null && petOpt != null) {
                                log.info("Successfully build data for Pet Management, proceed to transform data ");
                                return new ActivityLogDataContext.Builder()
                                        .owner(ownerOpt)
                                        .pet(petOpt)
                                        .build();
                            }
                        }
                    }
                    return null;

                }

                case REQUEST_MANAGEMENT -> {
                    if (activity.getTypeId() != null) {
                        log.info("Starting searching data for Request Management");
                        var request = requestSearch.searchByRequestId(activity.getTypeId());
                        if (request != null) {
                            log.info("Request Management: not null, proceed to search");
                            var petOpt = petSearch.getPetBoardingDetails(request.getPetId());
                            if (petOpt == null) return null;

                            return switch (request.getRequestType()) {
                                case BOARDING_EXTENSION -> {
                                    log.info("Starting searching data for Boarding Extension");
                                    var boardingResult = boardingSearch.findBoardingById(request.getBoardingId());
                                    var pricingOpt = pricingSearch.getBoardingPricingCqrs(request.getBoardingId());
                                    var extension = requestSearch.searchExtensionByRequestIdCqrs(request.getId());

                                    if (boardingResult.isSuccess() && pricingOpt.isPresent() && extension != null) {
                                        log.info("Successfully build data for Boarding Extension, proceed to transform data ");
                                        yield new ActivityLogDataContext.Builder()
                                                .boarding(boardingResult.getData())
                                                .pricing(pricingOpt.get())
                                                .extension(extension)
                                                .pet(petOpt)
                                                .build();
                                    }
                                    yield null;
                                }

                                case GROOMING_SERVICE -> {
                                    log.info("Starting searching data for Grooming Service");
                                    var grooming = requestSearch.searchGroomingByRequestIdCqrs(request.getId());
                                    if (grooming != null) {
                                        log.info("Successfully build data for Grooming Service, proceed to transform data ");
                                        yield new ActivityLogDataContext.Builder()
                                                .grooming(grooming)
                                                .pet(petOpt)
                                                .build();
                                    }
                                    yield null;
                                }

                                case PHOTO_REQUEST, VIDEO_REQUEST -> new ActivityLogDataContext.Builder()
                                        .pet(petOpt)
                                        .build();

                                default -> null;
                            };
                        }
                    }
                    return null;
                }

                default -> {
                    return null;
                }
            }
        } catch (PersistenceException | IllegalStateException |
                 NullPointerException ex) {
            // Optionally log the error for debugging:
            log.warn("Failed to build activity log context for activity {}: {}", activity.getId(), ex.getMessage());
        }

        return null;
    }


    private ActivityLogDTO transformLog(ActivityLogDomain domain, ActivityLogDataContext context) {
        return switch (domain.getActivityType()) {
            case BOARDING_MANAGEMENT -> ActivityLogDS.transformBoarding(domain, context);
            case PET_OWNER_MANAGEMENT -> ActivityLogDS.transformRegisterOwner(domain, context);
            case PET_MANAGEMENT -> ActivityLogDS.transformRegisterPet(domain, context);
            case REQUEST_MANAGEMENT -> {
                if (domain.getRequestType() == null) {
                    throw new PersistenceException("Request type is null for request management log");
                }
                yield switch (domain.getRequestType()) {
                    case PHOTO_REQUEST, VIDEO_REQUEST -> ActivityLogDS.transformRequestMedia(domain, context);
                    case BOARDING_EXTENSION -> ActivityLogDS.transformRequestExtension(domain, context);
                    case GROOMING_SERVICE -> ActivityLogDS.transformRequestGrooming(domain, context);
                    default -> null;
                };
            }
            default -> null;
        };
    }


}
