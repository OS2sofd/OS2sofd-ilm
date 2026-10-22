package dk.digitalidentity.OS2ilm.security.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The multipart body of an oversized logo upload can get parsed (and rejected)
 * as early as Spring Security's CsrfFilter, which runs before DispatcherServlet
 * ever sees the request. That means the resulting size-limit exception never
 * reaches a @ControllerAdvice/@ExceptionHandler - it has to be caught here,
 * wrapping the whole filter chain, to redirect back to a friendly error page.
 */
public class UploadSizeLimitFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		try {
			filterChain.doFilter(request, response);
		}
		catch (Exception ex) {
			if (!isSizeLimitExceeded(ex)) {
				if (ex instanceof ServletException servletException) {
					throw servletException;
				}
				if (ex instanceof IOException ioException) {
					throw ioException;
				}

				throw new ServletException(ex);
			}

			response.sendRedirect(request.getContextPath() + "/admin/cms-edit?key=cms.logo&fileTooLarge=true");
		}
	}

	private boolean isSizeLimitExceeded(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			String name = cause.getClass().getName();
			if (name.contains("MaxUploadSizeExceededException") || name.contains("FileSizeLimitExceededException") || name.contains("SizeLimitExceededException")) {
				return true;
			}
		}

		return false;
	}
}
