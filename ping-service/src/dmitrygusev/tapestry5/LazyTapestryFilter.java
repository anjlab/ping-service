package dmitrygusev.tapestry5;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.tapestry5.TapestryFilter;

public class LazyTapestryFilter implements Filter {

    private Filter tapestryFilter;
    
    private FilterConfig config;
    
    @Override
    public void init(FilterConfig config) throws ServletException
    {
        this.config = config;
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
         throws IOException, ServletException
    {
        String requestURI = ((HttpServletRequest) request).getRequestURI();
        
        if (requestURI.startsWith("/filters/") || requestURI.equalsIgnoreCase("/favicon.ico"))
        {
            return;
        }
        
        if (tapestryFilter == null)
        {
            tapestryFilter = new TapestryFilter();
            tapestryFilter.init(config);
        }
        
        tapestryFilter.doFilter(request, response, chain);
    }

    @Override
    public void destroy()
    {
        tapestryFilter.destroy();
    }

}
