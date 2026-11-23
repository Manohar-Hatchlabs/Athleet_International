package com.athleet.academy.student.dto;

import com.athleet.academy.student.domain.DominantSide;
import com.athleet.academy.student.domain.Gender;
import com.athleet.academy.student.domain.StudentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class StudentDtos {

    private StudentDtos() {
    }

    private static final String NAME = "^[\\p{L}][\\p{L} .'-]*$";
    private static final String TEXT = "^[\\p{L}\\p{N} .,'&()/#:-]*$";

    /** Fields match Acadware's Add Student form. */
    public record StudentRequest(
            @NotNull UUID centerId,
            @NotEmpty @Size(max = 5) Set<UUID> batchIds,
            @NotBlank @Size(max = 60) @Pattern(regexp = NAME, message = "letters only") String firstName,
            @Size(max = 60) @Pattern(regexp = "^[\\p{L} .'-]*$", message = "letters only") String lastName,
            @Email @Size(max = 254) String email,
            @NotNull Gender gender,
            @NotNull @Past LocalDate dateOfBirth,
            @Pattern(regexp = "^\\+?\\d{1,4}$", message = "invalid country code") String countryCode,
            @Pattern(regexp = "^\\d{6,15}$", message = "must be 6-15 digits") String mobile,
            @Size(max = 40) @Pattern(regexp = "^[A-Za-z0-9/-]*$", message = "letters, digits, / and - only") String uniqueId,
            @Pattern(regexp = "^(A|B|AB|O)[+-]$|^$", message = "invalid blood group") String bloodGroup,
            @Size(max = 100) @Pattern(regexp = TEXT, message = "contains invalid characters") String country,
            @Size(max = 100) @Pattern(regexp = TEXT, message = "contains invalid characters") String state,
            @Size(max = 100) @Pattern(regexp = TEXT, message = "contains invalid characters") String city,
            @Size(max = 300) String address,
            @Pattern(regexp = "^[A-Za-z0-9 -]{0,12}$", message = "invalid pin code") String pinCode,
            @Size(max = 150) @Pattern(regexp = TEXT, message = "contains invalid characters") String schoolName,
            @Size(max = 300) String schoolAddress,
            @Size(max = 20) @Pattern(regexp = TEXT, message = "contains invalid characters") String grade,
            DominantSide dominantSide,
            @Size(max = 1000) String medicalCondition) {
    }

    public record DeregisterRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record ChangeBatchesRequest(@NotNull UUID centerId, @NotEmpty @Size(max = 5) Set<UUID> batchIds) {
    }

    public record NamedRef(UUID id, String name) {
    }

    /** Row in the student list (Acadware columns: Student Name, ID, Class Name, Batch, Age, Grade, Email Address). */
    public record StudentSummary(UUID id, long studentNo, String firstName, String lastName, NamedRef center,
                                 List<NamedRef> batches, Integer age, String grade, String email, StudentStatus status) {
    }

    public record StudentResponse(UUID id, long studentNo, NamedRef center, List<NamedRef> batches,
                                  String firstName, String lastName, String email, Gender gender, LocalDate dateOfBirth,
                                  Integer age, String countryCode, String mobile, String uniqueId, String bloodGroup,
                                  String country, String state, String city, String address, String pinCode,
                                  String schoolName, String schoolAddress, String grade, DominantSide dominantSide,
                                  String medicalCondition, StudentStatus status) {
    }
}
