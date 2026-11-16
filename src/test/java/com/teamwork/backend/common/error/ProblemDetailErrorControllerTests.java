package com.teamwork.backend.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Lỗi phát sinh ngoài controller được server chuyển tới {@code /error} kèm các thuộc tính
 * {@code jakarta.servlet.error.*}; test giả lập lượt chuyển đó.
 */
@WebMvcTest(ProblemDetailErrorController.class)
@WithMockUser
class ProblemDetailErrorControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void errorDispatchReturnsCommonFormatWithOriginalPath() throws Exception {
		this.mockMvc
			.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
				.requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/groups/99"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
			.andExpect(jsonPath("$.detail").value(CommonErrorCode.NOT_FOUND.defaultMessage()))
			.andExpect(jsonPath("$.instance").value("/api/groups/99"))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void serverErrorDispatchDoesNotExposeExceptionMessage() throws Exception {
		String body = this.mockMvc
			.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
				.requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/groups")
				.requestAttr(RequestDispatcher.ERROR_MESSAGE, "db-secret-host:5432")
				.requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("db-secret-host:5432")))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
			.andExpect(jsonPath("$.detail").value(CommonErrorCode.INTERNAL_ERROR.defaultMessage()))
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(body).doesNotContain("db-secret-host", "IllegalStateException", "trace");
	}

	@Test
	void statusOutsideContractUsesStatusNameAsCode() throws Exception {
		this.mockMvc
			.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 406)
				.requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/groups"))
			.andExpect(status().isNotAcceptable())
			.andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"));
	}

}
