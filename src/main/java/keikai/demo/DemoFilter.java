package keikai.demo;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import com.keikai.client.api.*;


//@WebFilter(filterName = "DemoFilter",
//urlPatterns = {"/javaClientDemo.jsp"})
public class DemoFilter implements Filter {

	Spreadsheet spreadsheet;
	static public String SPREADSHEET = "spreadsheet"; //the key to store Spreadsheet component
	
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		initSpreadsheet(request);
		File template = new File(request.getServletContext().getRealPath("/book/large.xlsx"));
		fillCellData(template);
		chain.doFilter(request, response);
	}


	private void initSpreadsheet(ServletRequest request) {
		spreadsheet = Keikai.newClient("http://114.34.173.199:8888");
		String jsAPI = spreadsheet.getURI("spreadsheet"); // you need to pass the DOM element id to it
		request.setAttribute("jsAPI", jsAPI);
		((HttpServletRequest)request).getSession().setAttribute(SPREADSHEET, spreadsheet);
	}

	private void fillCellData(File template){
		spreadsheet.ready(() -> {
			try{
				spreadsheet.imports("template.xlsx", template);
				for (int row = 1; row < 10; row++) {
					for (int col = 0; col < 10; col++) {
						spreadsheet.getRange(row, col).applyValue(row+","+col); // The value is either String or Number value 
					}
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
		});

	}

	public void destroy() {
	}

}
