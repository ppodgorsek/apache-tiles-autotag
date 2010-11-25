/**
 *
 */
package org.apache.tiles.autotag.jsp.runtime;

import static org.easymock.EasyMock.*;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.JspFragment;

import org.apache.tiles.request.ApplicationContext;
import org.apache.tiles.request.jsp.JspRequest;
import org.apache.tiles.request.util.ApplicationAccess;
import org.junit.Test;

/**
 * Tests {@link BodyTag}.
 *
 * @version $Rev$ $Date$
 */
public class BodyTagTest {

    /**
     * Test method for {@link org.apache.tiles.autotag.jsp.runtime.BodyTag#doTag()}.
     * @throws IOException If something goes wrong.
     */
    @Test
    public void testDoTag() throws IOException {
        PageContext pageContext = createMock(PageContext.class);
        JspFragment jspBody = createMock(JspFragment.class);
        BodyTag tag = createMockBuilder(BodyTag.class).createMock();
        ApplicationContext applicationContext = createMock(ApplicationContext.class);
        HttpServletRequest httpServletRequest = createMock(HttpServletRequest.class);
        HttpServletResponse httpServletResponse = createMock(HttpServletResponse.class);
        JspWriter jspWriter = createMock(JspWriter.class);

        expect(pageContext.getAttribute(ApplicationAccess.APPLICATION_CONTEXT_ATTRIBUTE,
                PageContext.APPLICATION_SCOPE)).andReturn(applicationContext);
        expect(pageContext.getRequest()).andReturn(httpServletRequest);
        expect(pageContext.getResponse()).andReturn(httpServletResponse);
        expect(pageContext.getOut()).andReturn(jspWriter);
        tag.execute(isA(JspRequest.class), isA(JspModelBody.class));

        replay(pageContext, jspBody, tag, applicationContext, httpServletRequest, httpServletResponse, jspWriter);
        tag.setJspContext(pageContext);
        tag.setJspBody(jspBody);
        tag.doTag();
        verify(pageContext, jspBody, tag, applicationContext, httpServletRequest, httpServletResponse, jspWriter);
    }

}
