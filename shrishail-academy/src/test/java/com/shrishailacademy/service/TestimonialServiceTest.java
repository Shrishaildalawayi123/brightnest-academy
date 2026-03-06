package com.shrishailacademy.service;

import com.shrishailacademy.dto.TestimonialRequest;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.Testimonial;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.repository.TestimonialRepository;
import com.shrishailacademy.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestimonialServiceTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private TestimonialRepository testimonialRepo;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private TestimonialService testimonialService;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT_ID, "test");
        testTenant = new Tenant();
        testTenant.setId(TENANT_ID);
        testTenant.setTenantKey("test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void addTestimonialShouldForceApprovedFalse() {
        TestimonialRequest request = new TestimonialRequest();
        request.setStudentName("Alice");
        request.setReview("Great experience!");
        request.setRating(5);

        when(tenantService.requireCurrentTenant()).thenReturn(testTenant);
        when(testimonialRepo.save(any(Testimonial.class))).thenAnswer(inv -> {
            Testimonial t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        Testimonial saved = testimonialService.addTestimonial(request);

        assertFalse(saved.isApproved());
        assertEquals("Alice", saved.getStudentName());
        assertEquals(5, saved.getRating());
        verify(testimonialRepo).save(any(Testimonial.class));
    }

    @Test
    void addTestimonialShouldClampRatingToRange() {
        TestimonialRequest request = new TestimonialRequest();
        request.setStudentName("Bob");
        request.setReview("Good");
        request.setRating(10); // above max

        when(tenantService.requireCurrentTenant()).thenReturn(testTenant);
        when(testimonialRepo.save(any(Testimonial.class))).thenAnswer(inv -> {
            Testimonial t = inv.getArgument(0);
            t.setId(2L);
            return t;
        });

        Testimonial saved = testimonialService.addTestimonial(request);

        assertEquals(5, saved.getRating());
    }

    @Test
    void addTestimonialShouldClampRatingMinimum() {
        TestimonialRequest request = new TestimonialRequest();
        request.setStudentName("Carol");
        request.setReview("Okay");
        request.setRating(0); // below min

        when(tenantService.requireCurrentTenant()).thenReturn(testTenant);
        when(testimonialRepo.save(any(Testimonial.class))).thenAnswer(inv -> {
            Testimonial t = inv.getArgument(0);
            t.setId(3L);
            return t;
        });

        Testimonial saved = testimonialService.addTestimonial(request);

        assertEquals(1, saved.getRating());
    }

    @Test
    void addTestimonialShouldSanitizeInput() {
        TestimonialRequest request = new TestimonialRequest();
        request.setStudentName("<script>alert(1)</script> Dave");
        request.setReview("Nice <b>course</b>");
        request.setRating(4);

        when(tenantService.requireCurrentTenant()).thenReturn(testTenant);
        when(testimonialRepo.save(any(Testimonial.class))).thenAnswer(inv -> {
            Testimonial t = inv.getArgument(0);
            t.setId(4L);
            return t;
        });

        Testimonial saved = testimonialService.addTestimonial(request);

        assertFalse(saved.getStudentName().contains("<script>"));
    }

    @Test
    void getApprovedTestimonialsShouldReturnOnlyApproved() {
        Testimonial t = new Testimonial();
        t.setApproved(true);
        when(testimonialRepo.findByTenantIdAndApprovedTrueOrderByCreatedAtDesc(TENANT_ID)).thenReturn(List.of(t));

        List<Testimonial> result = testimonialService.getApprovedTestimonials();

        assertEquals(1, result.size());
        assertTrue(result.get(0).isApproved());
    }

    @Test
    void getAllTestimonialsShouldReturnAll() {
        when(testimonialRepo.findByTenantIdOrderByCreatedAtDesc(TENANT_ID))
                .thenReturn(List.of(new Testimonial(), new Testimonial()));

        List<Testimonial> result = testimonialService.getAllTestimonials();

        assertEquals(2, result.size());
    }

    @Test
    void toggleApprovalShouldFlipState() {
        Testimonial t = new Testimonial();
        t.setId(1L);
        t.setApproved(false);

        when(testimonialRepo.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(t));
        when(testimonialRepo.save(any(Testimonial.class))).thenAnswer(inv -> inv.getArgument(0));

        Testimonial toggled = testimonialService.toggleApproval(1L);

        assertTrue(toggled.isApproved());
    }

    @Test
    void toggleApprovalShouldThrowWhenNotFound() {
        when(testimonialRepo.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> testimonialService.toggleApproval(99L));
    }

    @Test
    void deleteTestimonialShouldDeleteExisting() {
        when(testimonialRepo.existsByIdAndTenantId(1L, TENANT_ID)).thenReturn(true);

        testimonialService.deleteTestimonial(1L);

        verify(testimonialRepo).deleteByIdAndTenantId(1L, TENANT_ID);
    }

    @Test
    void deleteTestimonialShouldThrowWhenNotFound() {
        when(testimonialRepo.existsByIdAndTenantId(99L, TENANT_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> testimonialService.deleteTestimonial(99L));
    }
}
