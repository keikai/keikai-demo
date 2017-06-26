package keikai.demo;

public class Configuration {
	public static final String KEIKAI_SERVER = "http://114.34.173.199:8888"; 

	public static String[] borderIndexList = { "none", "diagonalDown", "diagonalUp", "edgeBottom", "edgeLeft", "edgeRight", "edgeTop",
	"insideHorizontal", "insideVertical" };
	public static String[] borderLineStyleList = { "none", "thin", "medium", "dashed", "dotted", "thick", "double", "hair", "mediumDashed", "dashDot",
		"mediumDashDot", "dashDotDot", "mediumDashDotDot", "slantDashDot"};
	public static String[] autoFilterList = { "and", "bottom10Items", "bottom10Percent", "filterCellColor", "filterDynamic",
	"filterFontColor", "filterIcon", "filterValues", "top10Items", "top10Percent" };
	public static String[] borderWeightList = { "hairline", "medium", "thick", "thin" };
	public static String[] underlineStyles = {"none", "single", "double", "singleAccounting", "doubleAccounting"};
	public static String[] fontSizes = {"6","8", "10", "12", "14", "16", "18", "22","36","72"};
}
