package au.com.dius.pact.provider.spring;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class BookTypeArgumentResolver implements HandlerMethodArgumentResolver {

    public BookTypeArgumentResolver() {}

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getGenericParameterType().equals(BookType.class);
    }

    @Override
    public BookType resolveArgument(MethodParameter methodParameter, ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

		    String type = webRequest.getParameter("type");
		    return new BookType(type);
	  }
}
