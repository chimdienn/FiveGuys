package com.example.data.mapper

import com.example.data.local.AdventureSessionEntity
import com.example.data.local.ChallengeSubmissionEntity
import com.example.data.local.CoinTransactionEntity
import com.example.data.local.ConnectionEntity
import com.example.data.local.ConversationEntity
import com.example.data.local.DailyChallengeEntity
import com.example.data.local.EarnedBadgeEntity
import com.example.data.local.GearItemEntity
import com.example.data.local.MessageEntity
import com.example.data.local.ProfileEntity
import com.example.data.local.ReadinessEntity
import com.example.data.local.TrailEntity
import com.example.data.local.TrailMomentEntity
import com.example.data.local.TripEntity
import com.example.data.local.TripMemberEntity
import com.example.domain.model.ActivityType
import com.example.domain.model.AdventureSession
import com.example.domain.model.BadgeId
import com.example.domain.model.ChallengeSubmission
import com.example.domain.model.CoinTransaction
import com.example.domain.model.Connection
import com.example.domain.model.ConnectionStatus
import com.example.domain.model.Conversation
import com.example.domain.model.ConversationType
import com.example.domain.model.DailyChallenge
import com.example.domain.model.Difficulty
import com.example.domain.model.EarnedBadge
import com.example.domain.model.ExperienceLevel
import com.example.domain.model.FitnessLevel
import com.example.domain.model.GearItem
import com.example.domain.model.GeoPoint
import com.example.domain.model.Message
import com.example.domain.model.MomentCategory
import com.example.domain.model.MomentVisibility
import com.example.domain.model.PreferredPace
import com.example.domain.model.Readiness
import com.example.domain.model.ReadinessItem
import com.example.domain.model.SessionStatus
import com.example.domain.model.Skill
import com.example.domain.model.SocialStyle
import com.example.domain.model.SubmissionState
import com.example.domain.model.Trail
import com.example.domain.model.TrailMoment
import com.example.domain.model.Trip
import com.example.domain.model.TripMember
import com.example.domain.model.TripMemberStatus
import com.example.domain.model.TripRole
import com.example.domain.model.TripStatus
import com.example.domain.model.UserProfile

/**
 * Conversions between Room storage records and domain models.
 *
 * Decoding is defensive: an unrecognised enum name falls back to a sensible default
 * rather than throwing, so a row written by a newer build cannot brick an older one.
 */

private inline fun <reified T : Enum<T>> String?.toEnumOr(default: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: default

// ----- Profile ------------------------------------------------------------------------

fun ProfileEntity.toDomain(): UserProfile = UserProfile(
    uid = uid,
    displayName = displayName,
    avatarUrl = avatarUrl,
    bio = bio,
    birthYear = birthYear,
    gender = gender,
    homeArea = homeArea,
    fitnessLevel = fitnessLevel.toEnumOr(FitnessLevel.MODERATE),
    experienceLevel = experienceLevel.toEnumOr(ExperienceLevel.BEGINNER),
    preferredPace = preferredPace.toEnumOr(PreferredPace.MODERATE),
    socialStyles = Codec.decodeEnums(socialStyles) { name -> SocialStyle.entries.firstOrNull { it.name == name } },
    interests = Codec.decodeEnums(interests) { name -> ActivityType.entries.firstOrNull { it.name == name } },
    skills = Codec.decodeEnums(skills) { name -> Skill.entries.firstOrNull { it.name == name } },
    avatarColorHex = avatarColorHex,
    onboardingComplete = onboardingComplete,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun UserProfile.toEntity(): ProfileEntity = ProfileEntity(
    uid = uid,
    displayName = displayName,
    displayNameLower = displayName.lowercase(),
    avatarUrl = avatarUrl,
    bio = bio,
    birthYear = birthYear,
    gender = gender,
    homeArea = homeArea,
    fitnessLevel = fitnessLevel.name,
    experienceLevel = experienceLevel.name,
    preferredPace = preferredPace.name,
    socialStyles = Codec.encodeEnums(socialStyles),
    interests = Codec.encodeEnums(interests),
    skills = Codec.encodeEnums(skills),
    avatarColorHex = avatarColorHex,
    onboardingComplete = onboardingComplete,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// ----- Trail --------------------------------------------------------------------------

fun TrailEntity.toDomain(): Trail = Trail(
    id = id,
    name = name,
    region = region,
    stateOrCountry = stateOrCountry,
    activityTypes = Codec.decodeEnums(activityTypes) { n -> ActivityType.entries.firstOrNull { it.name == n } }
        .ifEmpty { setOf(ActivityType.HIKING) },
    description = description,
    difficulty = difficulty.toEnumOr(Difficulty.MODERATE),
    distanceKm = distanceKm,
    elevationGainM = elevationGainM,
    estimatedMinutes = estimatedMinutes,
    start = GeoPoint(startLat, startLng),
    route = Codec.decodePoints(route),
    waypoints = Codec.decodeWaypoints(waypoints),
    imageUrl = imageUrl,
    tags = Codec.decodeList(tags),
    isExposed = isExposed,
    isShaded = isShaded,
    rating = rating,
    reviewCount = reviewCount,
    highlights = Codec.decodeList(highlights),
    recommendedGear = Codec.decodeList(recommendedGear),
    createdAt = createdAt
)

fun Trail.toEntity(): TrailEntity = TrailEntity(
    id = id,
    name = name,
    region = region,
    stateOrCountry = stateOrCountry,
    activityTypes = Codec.encodeEnums(activityTypes),
    description = description,
    difficulty = difficulty.name,
    distanceKm = distanceKm,
    elevationGainM = elevationGainM,
    estimatedMinutes = estimatedMinutes,
    startLat = start.latitude,
    startLng = start.longitude,
    route = Codec.encodePoints(route),
    waypoints = Codec.encodeWaypoints(waypoints),
    imageUrl = imageUrl,
    tags = Codec.encodeList(tags),
    isExposed = isExposed,
    isShaded = isShaded,
    rating = rating,
    reviewCount = reviewCount,
    highlights = Codec.encodeList(highlights),
    recommendedGear = Codec.encodeList(recommendedGear),
    createdAt = createdAt
)

// ----- Connection ---------------------------------------------------------------------

fun ConnectionEntity.toDomain(): Connection = Connection(
    id = id,
    requesterId = requesterId,
    addresseeId = addresseeId,
    status = status.toEnumOr(ConnectionStatus.PENDING),
    createdAt = createdAt,
    respondedAt = respondedAt
)

fun Connection.toEntity(): ConnectionEntity = ConnectionEntity(
    id = id,
    requesterId = requesterId,
    addresseeId = addresseeId,
    status = status.name,
    createdAt = createdAt,
    respondedAt = respondedAt
)

// ----- Trip ---------------------------------------------------------------------------

fun TripEntity.toDomain(): Trip = Trip(
    id = id,
    creatorId = creatorId,
    trailId = trailId,
    trailName = trailName,
    title = title,
    startsAt = startsAt,
    meetingPoint = meetingPoint,
    participantLimit = participantLimit,
    carpoolNotes = carpoolNotes,
    foodNotes = foodNotes,
    generalNotes = generalNotes,
    emergencyNotes = emergencyNotes,
    status = status.toEnumOr(TripStatus.PLANNING),
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt
)

fun Trip.toEntity(): TripEntity = TripEntity(
    id = id,
    creatorId = creatorId,
    trailId = trailId,
    trailName = trailName,
    title = title,
    startsAt = startsAt,
    meetingPoint = meetingPoint,
    participantLimit = participantLimit,
    carpoolNotes = carpoolNotes,
    foodNotes = foodNotes,
    generalNotes = generalNotes,
    emergencyNotes = emergencyNotes,
    status = status.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt
)

fun TripMemberEntity.toDomain(): TripMember = TripMember(
    tripId = tripId,
    uid = uid,
    displayName = displayName,
    role = role.toEnumOr(TripRole.PARTICIPANT),
    status = status.toEnumOr(TripMemberStatus.INVITED),
    joinedAt = joinedAt,
    attended = attended
)

fun TripMember.toEntity(): TripMemberEntity = TripMemberEntity(
    tripId = tripId,
    uid = uid,
    displayName = displayName,
    role = role.name,
    status = status.name,
    joinedAt = joinedAt,
    attended = attended
)

fun GearItemEntity.toDomain(): GearItem = GearItem(
    id = id,
    tripId = tripId,
    name = name,
    category = category,
    quantity = quantity,
    assignedToUid = assignedToUid,
    assignedToName = assignedToName,
    isPacked = isPacked,
    isEssential = isEssential
)

fun GearItem.toEntity(): GearItemEntity = GearItemEntity(
    id = id,
    tripId = tripId,
    name = name,
    category = category,
    quantity = quantity,
    assignedToUid = assignedToUid,
    assignedToName = assignedToName,
    isPacked = isPacked,
    isEssential = isEssential
)

fun ReadinessEntity.toDomain(): Readiness = Readiness(
    tripId = tripId,
    uid = uid,
    checkedItems = Codec.decodeEnums(checkedItems) { n -> ReadinessItem.entries.firstOrNull { it.name == n } },
    confidence = confidence,
    notes = notes,
    updatedAt = updatedAt
)

fun Readiness.toEntity(): ReadinessEntity = ReadinessEntity(
    tripId = tripId,
    uid = uid,
    checkedItems = Codec.encodeEnums(checkedItems),
    confidence = confidence,
    notes = notes,
    updatedAt = updatedAt
)

// ----- Messaging ----------------------------------------------------------------------

fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    type = type.toEnumOr(ConversationType.DIRECT),
    memberIds = Codec.decodeList(memberIds),
    title = title,
    tripId = tripId,
    lastMessagePreview = lastMessagePreview,
    lastMessageAt = lastMessageAt,
    lastMessageSenderId = lastMessageSenderId,
    lastReadAt = Codec.decodeLongMap(lastReadAt)
)

fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    type = type.name,
    memberIds = Codec.encodeList(memberIds),
    title = title,
    tripId = tripId,
    lastMessagePreview = lastMessagePreview,
    lastMessageAt = lastMessageAt,
    lastMessageSenderId = lastMessageSenderId,
    lastReadAt = Codec.encodeLongMap(lastReadAt)
)

fun MessageEntity.toDomain(): Message = Message(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    senderName = senderName,
    text = text,
    sentAt = sentAt,
    isSystem = isSystem
)

fun Message.toEntity(): MessageEntity = MessageEntity(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    senderName = senderName,
    text = text,
    sentAt = sentAt,
    isSystem = isSystem
)

// ----- Trail moment -------------------------------------------------------------------

fun TrailMomentEntity.toDomain(): TrailMoment = TrailMoment(
    id = id,
    creatorId = creatorId,
    creatorName = creatorName,
    trailId = trailId,
    tripId = tripId,
    latitude = latitude,
    longitude = longitude,
    category = category.toEnumOr(MomentCategory.NOTE),
    description = description,
    photoUrl = photoUrl,
    visibility = visibility.toEnumOr(MomentVisibility.PUBLIC),
    createdAt = createdAt,
    upvotes = upvotes
)

fun TrailMoment.toEntity(): TrailMomentEntity = TrailMomentEntity(
    id = id,
    creatorId = creatorId,
    creatorName = creatorName,
    trailId = trailId,
    tripId = tripId,
    latitude = latitude,
    longitude = longitude,
    category = category.name,
    description = description,
    photoUrl = photoUrl,
    visibility = visibility.name,
    createdAt = createdAt,
    upvotes = upvotes
)

// ----- Challenges & coins -------------------------------------------------------------

fun DailyChallengeEntity.toDomain(): DailyChallenge = DailyChallenge(
    id = id,
    uid = uid,
    challengeId = challengeId,
    dateKey = dateKey,
    progress = progress,
    target = target,
    completedAt = completedAt,
    rewardedAt = rewardedAt
)

fun DailyChallenge.toEntity(): DailyChallengeEntity = DailyChallengeEntity(
    id = id,
    uid = uid,
    challengeId = challengeId,
    dateKey = dateKey,
    progress = progress,
    target = target,
    completedAt = completedAt,
    rewardedAt = rewardedAt
)

fun ChallengeSubmissionEntity.toDomain(): ChallengeSubmission = ChallengeSubmission(
    id = id,
    uid = uid,
    dailyChallengeId = dailyChallengeId,
    challengeId = challengeId,
    photoUrl = photoUrl,
    state = state.toEnumOr(SubmissionState.PENDING),
    confidence = confidence,
    explanation = explanation,
    submittedAt = submittedAt,
    verifiedAt = verifiedAt
)

fun ChallengeSubmission.toEntity(): ChallengeSubmissionEntity = ChallengeSubmissionEntity(
    id = id,
    uid = uid,
    dailyChallengeId = dailyChallengeId,
    challengeId = challengeId,
    photoUrl = photoUrl,
    state = state.name,
    confidence = confidence,
    explanation = explanation,
    submittedAt = submittedAt,
    verifiedAt = verifiedAt
)

fun CoinTransactionEntity.toDomain(): CoinTransaction = CoinTransaction(
    id = id,
    uid = uid,
    amount = amount,
    reason = reason,
    challengeId = challengeId,
    referenceId = referenceId,
    idempotencyKey = idempotencyKey,
    createdAt = createdAt
)

fun CoinTransaction.toEntity(): CoinTransactionEntity = CoinTransactionEntity(
    id = id,
    uid = uid,
    amount = amount,
    reason = reason,
    challengeId = challengeId,
    referenceId = referenceId,
    idempotencyKey = idempotencyKey,
    createdAt = createdAt
)

fun EarnedBadgeEntity.toDomain(): EarnedBadge? {
    val id = BadgeId.entries.firstOrNull { it.name == badgeId } ?: return null
    return EarnedBadge(uid = uid, badgeId = id, earnedAt = earnedAt)
}

// ----- Session ------------------------------------------------------------------------

fun AdventureSessionEntity.toDomain(): AdventureSession = AdventureSession(
    id = id,
    uid = uid,
    tripId = tripId,
    trailId = trailId,
    startedAt = startedAt,
    completedAt = completedAt,
    distanceKm = distanceKm,
    durationMinutes = durationMinutes,
    momentCount = momentCount,
    companionCount = companionCount,
    status = status.toEnumOr(SessionStatus.ACTIVE)
)

fun AdventureSession.toEntity(): AdventureSessionEntity = AdventureSessionEntity(
    id = id,
    uid = uid,
    tripId = tripId,
    trailId = trailId,
    startedAt = startedAt,
    completedAt = completedAt,
    distanceKm = distanceKm,
    durationMinutes = durationMinutes,
    momentCount = momentCount,
    companionCount = companionCount,
    status = status.name
)
