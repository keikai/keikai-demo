package keikai.demo;


import io.keikai.client.api.*;

import javax.servlet.*;
import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import java.io.*;

import static io.keikai.client.api.Border.Style;
import static io.keikai.client.api.Borders.BorderIndex;

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
		File template = new File(request.getServletContext().getRealPath("/book/template.xlsx"));
		fillCellData(template);
		chain.doFilter(request, response);
	}


	private void initSpreadsheet(ServletRequest request) {
		spreadsheet = Keikai.newClient(Configuration.INTERNAL_KEIKAI_SERVER);
		String jsAPI = spreadsheet.getURI("spreadsheet"); // you need to pass the DOM element id to it
		request.setAttribute("jsAPI", jsAPI);
		((HttpServletRequest)request).getSession().setAttribute(SPREADSHEET, spreadsheet);
	}

	private void fillCellData(File template){
//		spreadsheet.ready(() -> {
//			try{
//				spreadsheet.imports("template.xlsx", template);
//				for (int row = 1; row < 10; row++) {
//					for (int col = 0; col < 10; col++) {
//						spreadsheet.getRange(row, col).applyValue(row+","+col); // The value is either String or Number value
//					}
//				}
//				applyBorders();
//				applyFont();
//			}catch (Exception e) {
//				e.printStackTrace();
//			}
//		});

	}

	private void applyFont() {
		Range range = spreadsheet.getRange("A1:B10");
		CellStyle cellStyle = range.createCellStyle();
		Font font = cellStyle.createFont();
		font.setBold(true);
		font.setName("Calibri");
		cellStyle.setFont(font);
		range.setCellStyle(cellStyle);
	}

	private void applyBorders() {
		Range range = spreadsheet.getRange("A1:B10");
		Borders borders = range.createBorders(BorderIndex.EdgeBottom);
		borders.setStyle(Style.Thin);
		borders.setColor("#363636");
		range.setBorders(borders);
	}

	public void destroy() {
	}

}
