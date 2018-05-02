
package mapgen_explorer.utils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

public class JsonFormatter {

	static void indente(int depth, Writer out) throws IOException {
		for (int i = 0; i < depth; i++) {
			out.write(' ');
			out.write(' ');
		}
	}

	public static void writeJSONString(Object value, int depth, Writer out) throws IOException {
		if (value == null) {
			out.write("null");
			return;
		}

		if (value instanceof String) {
			out.write('\"');
			out.write(JSONValue.escape((String) value));
			out.write('\"');
			return;
		}

		if (value instanceof Double) {
			if (((Double) value).isInfinite() || ((Double) value).isNaN())
				out.write("null");
			else
				out.write(value.toString());
			return;
		}

		if (value instanceof Float) {
			if (((Float) value).isInfinite() || ((Float) value).isNaN())
				out.write("null");
			else
				out.write(value.toString());
			return;
		}

		if (value instanceof Number) {
			out.write(value.toString());
			return;
		}

		if (value instanceof Boolean) {
			out.write(value.toString());
			return;
		}
		/*
				if ((value instanceof JSONStreamAware)) {
					((JSONStreamAware) value).writeJSONString(out);
					return;
				}
		
				if ((value instanceof JSONAware)) {
					out.write(((JSONAware) value).toJSONString());
					return;
				}
		*/
		/*
				if (value instanceof JSONArray) {
					writeJSONString((JSONArray) value, out);
					return;
				}
		
				if (value instanceof JSONObject) {
					writeJSONString((JSONObject) value, out);
					return;
				}
		*/
		if (value instanceof Map) {
			StringWriter inline_writter = new StringWriter();
			writeJSONString((Map) value, -1, inline_writter);
			String inline_string = inline_writter.toString();
			if (inline_string.length() <= 120) {
				out.write(inline_string.replace('\n', ' '));
			} else {
				writeJSONString((Map) value, depth, out);
			}
			return;
		}

		if (value instanceof Collection) {
			StringWriter inline_writter = new StringWriter();
			writeJSONString((Collection) value, -1, inline_writter);
			String inline_string = inline_writter.toString();
			if (inline_string.length() <= 120) {
				out.write(inline_string.replace('\n', ' '));
			} else {
				writeJSONString((Collection) value, depth, out);
			}
			return;
		}

		if (value instanceof byte[]) {
			JSONArray.writeJSONString((byte[]) value, out);
			return;
		}

		if (value instanceof short[]) {
			JSONArray.writeJSONString((short[]) value, out);
			return;
		}

		if (value instanceof int[]) {
			JSONArray.writeJSONString((int[]) value, out);
			return;
		}

		if (value instanceof long[]) {
			JSONArray.writeJSONString((long[]) value, out);
			return;
		}

		if (value instanceof float[]) {
			JSONArray.writeJSONString((float[]) value, out);
			return;
		}

		if (value instanceof double[]) {
			JSONArray.writeJSONString((double[]) value, out);
			return;
		}

		if (value instanceof boolean[]) {
			JSONArray.writeJSONString((boolean[]) value, out);
			return;
		}

		if (value instanceof char[]) {
			JSONArray.writeJSONString((char[]) value, out);
			return;
		}

		if (value instanceof Object[]) {
			writeJSONString((Object[]) value, depth, out);
			return;
		}

		out.write(value.toString());
	}

	public static void writeJSONString(Object[] array, int depth, Writer out) throws IOException {
		if (array == null) {
			out.write("null");
		} else if (array.length == 0) {
			out.write("[]");
		} else {
			out.write("[\n");
			indente(depth + 1, out);
			writeJSONString(array[0], depth + 1, out);
			for (int i = 1; i < array.length; i++) {
				out.write(",\n");
				indente(depth + 1, out);
				writeJSONString(array[i], depth + 1, out);
			}
			out.write("\n");
			indente(depth, out);
			out.write("]");
		}
	}

	public static void writeJSONString(Collection collection, int depth, Writer out)
			throws IOException {
		if (collection == null) {
			out.write("null");
			return;
		}

		boolean first = true;
		Iterator iter = collection.iterator();

		out.write("[\n");
		while (iter.hasNext()) {
			if (first)
				first = false;
			else
				out.write(",\n");
			indente(depth + 1, out);
			Object value = iter.next();
			if (value == null) {
				out.write("null");
				continue;
			}

			writeJSONString(value, depth + 1, out);
		}
		out.write("\n");
		indente(depth, out);
		out.write("]");
	}

	public static void writeJSONString(Map map, int depth, Writer out) throws IOException {
		if (map == null) {
			out.write("null");
			return;
		}

		boolean first = true;
		Iterator iter = map.entrySet().iterator();

		out.write("{\n");
		while (iter.hasNext()) {
			if (first)
				first = false;
			else
				out.write(",\n");
			indente(depth + 1, out);
			Map.Entry entry = (Map.Entry) iter.next();
			out.write('\"');
			out.write(JSONObject.escape(String.valueOf(entry.getKey())));
			out.write('\"');
			out.write(':');
			out.write(' ');
			writeJSONString(entry.getValue(), depth + 1, out);
		}
		out.write("\n");
		indente(depth, out);
		out.write("}");
	}

	// Convert the json object "src" to string while following the same structure as model.
	public static String formatJson(Object content) {
		try {
			StringWriter writer = new StringWriter();
			writeJSONString(content, 0, writer);
			return writer.toString();
		} catch (Exception e) {
			return content.toString();
		}
	}

	@Test
	public void basicTest() throws ParseException {
		String raw_v1 = String.join("\n", "[", "  {", "    \"type\": \"foo\",",
				"    \"id\": \"example\",", "    \"short_array\": [ 1, 2, 3, 4, 5 ],",
				"    \"short_object\": { \"item_a\": \"a\", \"item_b\": \"b\" },",
				"    \"long_array\": [ \"a really long string to illustrate line wrapping, \", \"which occurs if the line is longer than 120 characters\" ],",
				"    \"nested_array\": [", "      [", "        [ \"item1\", \"value1\" ],",
				"        [ \"item2\", \"value2\" ],", "        [ \"item3\", \"value3\" ],",
				"        [ \"item4\", \"value4\" ],", "        [ \"item5\", \"value5\" ],",
				"        [ \"item6\", \"value6\" ]", "      ]", "    ]", "  }", "]");
		String raw_v2 = raw_v1.replace("\n", "").replace(" ", "");
		JSONParser parser = new JSONParser();
		JSONArray parsed_v2 = (JSONArray) parser.parse(raw_v2);
		String formatted = formatJson(parsed_v2);
		assertEquals(raw_v1, formatted);
	}

}
