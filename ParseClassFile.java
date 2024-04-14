import java.util.Map;

import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.io.IOException;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * ParseClassFile
 * convert java file to class file for JVM
 */
public class ParseClassFile {
	private static int pointer = 8;
	private static Map<String, Object> tokens = new HashMap<String, Object>();
	private static List<Object> constants = new ArrayList<Object>();

	public static Map<String, Object> parseClassFile(String filename) throws IOException {
		var file = Files.readAllBytes(Paths.get(filename));

		// Magic number
		pointer = 4;
		LogUtils.printHex("magic", new byte[]{file[0], file[1], file[2], file[3]});
		tokens.put("versionMajor", getShort(file));
		tokens.put("versionMinor", getShort(file));

		// constant pool
		constants = getVariablesArray(file, (byte) 0);
		tokens.put("constantPool", constants);

		tokens.put("accessFlags", convertAccessFlagsToString((short) getShort(file)));
		tokens.put("thisClass", constants.get((short) constants.get(getShort(file)-1)-1));
		tokens.put("superClass", constants.get((short) constants.get(getShort(file)-1)-1));

		tokens.put("interfaces", interfaceConvert(file));
		tokens.put("fields", fieldConvert(file));
		tokens.put("methods", methodConvert(file));
		tokens.put("attributes", attributeConvert(file));

		LogUtils.printTokens(tokens);
		return tokens;
	}
	public static boolean checkByte(short b, int checker) {
		return (b & checker) == checker;
	}
	public static String convertAccessFlagsToString(short accessFlags) {
		StringBuilder sb = new StringBuilder();
		if (checkByte(accessFlags, 0x0001)) sb.append("public ");
		if (checkByte(accessFlags, 0x0002)) sb.append("private ");
		if (checkByte(accessFlags, 0x0004)) sb.append("protected ");
		if (checkByte(accessFlags, 0x0008)) sb.append("static ");
		if (checkByte(accessFlags, 0x0010)) sb.append("final ");
		if (checkByte(accessFlags, 0x0040)) sb.append("volatile ");
		if (checkByte(accessFlags, 0x0080)) sb.append("transient ");
		if (checkByte(accessFlags, 0x1000)) sb.append("synthetic ");
		if (checkByte(accessFlags, 0x4000)) sb.append("enum ");

		return sb.toString();
	}

	public static List<Object> getVariablesArray(byte[] content, byte type) {
		int count = (Byte.toUnsignedInt(content[pointer++]) << 8) | Byte.toUnsignedInt(content[pointer++]);

		var list = new ArrayList<Object>();
		LogUtils.printHex("count: "+count, new byte[] {content[pointer-2], content[pointer-1]});
		
		if (type == 0) {
			for (int i = 0; i < (count-1); i++) {
				var x = (poolConvert(content));
				list.add(x);
			}
			System.out.println("ending pool conversion");
		}
		return list;
	}
	public static record FieldInfo(String accessFlags, String name_index, String descriptor_index, short attributes_count, Map<Short, Byte[]> attributes) {}
	public static List<FieldInfo> fieldConvert(byte[] content) {
		var size = getShort(content);
		var list = new ArrayList<FieldInfo>();
		for (int i = 0; i < size; i++) {
			var accessFlag = convertAccessFlagsToString((short) (getShort(content) - ((short) 1)));
			var name_index = (String) constants.get(getShort(content) - 1);
			var descriptor_index = (String) constants.get((short) (getShort(content) - ((short) 1)));
			var attributes_count = getShort(content);

			var attributes = new HashMap<Short, Byte[]>();
			for (int j = 0; j < attributes_count; j++) {
				var attribute_name_index = (short) (content[pointer++]<<8 | content[pointer++]);
				var attributes_length = content[pointer++]<<24 | content[pointer++]<<16 | content[pointer++]<<8 | content[pointer++];
				var info = new Byte[attributes_length];
				for (int k = 0; k < attributes_length; k++)
					info[j] = content[pointer++];
				attributes.put(attribute_name_index, info);
			}
			list.add(new FieldInfo(accessFlag, name_index, descriptor_index, attributes_count, attributes));
		}
		return list;
	}
	public static List<String> interfaceConvert(byte[] content) {
		var size = getShort(content);
		try {
			var list = new ArrayList<String>();
			for (int i = 0; i < size; i++) {
				var key = ((short) (content[pointer++]<<8 | content[pointer++]));
				System.out.println("interface id: '"+key+"'");
				list.add((String) constants.get((Integer) constants.get(key-1)-1));
			}
			return list;
		} catch (Exception e) {
			System.out.println("Error parsing class file named: main.class");
			System.out.println(e.getMessage());
			System.out.println(e.getStackTrace());
		}
		return List.of();
	}
	private record MethodInfo(String accessFlags, String name_index, 
			String descriptor_index, short attributes_count, List<AttributeInfo> attributes) {}

	public static List<MethodInfo> methodConvert(byte[] content) {
		var size = getShort(content);
		var list = new ArrayList<MethodInfo>();
		System.out.println("Methods count: " + size);
		var translator = new ByteCodeConvertor();

		for (int i = 0; i < size; i++) {
			var accessFlag = convertAccessFlagsToString(getShort(content));
			var name = (String) constants.get(getShort(content) - 1);
			var descriptor = (String) constants.get(getShort(content) - 1);

			var methodAttr = attributeConvert(content);
			System.out.println("Method name: " + name + " descriptor: " + 
					descriptor + " attributes count: " + methodAttr.size());

			methodAttr.stream().forEach(attr -> {
				list.add(new MethodInfo(accessFlag, name, 
							descriptor, (short) methodAttr.size(), methodAttr));

				if (attr.name_index.equals("Code")) {
					System.out.println("Code attribute: " + attr.length);
					translator.instructionConvert(attr.value, constants);
				} else {
					LogUtils.printHex(attr.name_index + " " + attr.length + " ", attr.value);
				}
			});
		}

		return list;
	}
	private record AttributeInfo(String name_index, int length, byte[] value) {}
	public static List<AttributeInfo> attributeConvert(byte[] content, int pointerOutside) {
		// still letting use this function out of scope
		var size = getShort(content, pointerOutside);

		var list = new ArrayList<AttributeInfo>();

		for (int i = 0; i < size; i++) {
			var name_index = getShort(content, pointerOutside);
			System.out.println("attribute name index: " + name_index);
			String name;
			try {
				name = String.valueOf(constants.get(name_index - 1));
				System.out.println("attribute name: " + name);
			} catch (Exception e) {
				name = Integer.toString((Integer) constants.get(name_index - 1));
			}
			int length = getInteger(content, pointer);
			var value = new byte[length];
			for (int j = 0; j < length; j++) value[j] = content[pointerOutside++];
			list.add(new AttributeInfo(name, length, value));
			LogUtils.printHex("attribute name: " + name + " length: " + length, value);
		}

		return list;
	}
	
	// should be move to another class
	public static int getShort(byte[] content, int pointerOutside) {
		return Byte.toUnsignedInt(content[pointerOutside++]) << 8 | 
			Byte.toUnsignedInt(content[pointerOutside++]);
	}
	public static int getInteger(byte[] content, int pointerOutside) {
		return Byte.toUnsignedInt(content[pointerOutside++]) << 24 | Byte.toUnsignedInt(content[pointerOutside++]) << 16 |
			Byte.toUnsignedInt(content[pointerOutside++]) << 8 | Byte.toUnsignedInt(content[pointerOutside++]);
	}
	/*
	 * returns a list of attributes
	 *
	 * attribute_info { 
	 *		u2 attribute_name_index; 
	 *		u4 attribute_length; 
	 *		u1 info[attribute_length]; 
	 * }
	*/
	public static List<AttributeInfo> attributeConvert(byte[] content) {
		// return attributeConvert(content, pointer);
		var size = getShort(content);
		var list = new ArrayList<AttributeInfo>();

		for (int i = 0; i < size; i++) {
			var name_index = getShort(content);
			System.out.println("attribute name index: " + name_index);
			var name = (String) constants.get(name_index - 1);
			int length = getInteger(content);
			var value = new byte[length];
			for (int j = 0; j < length; j++) value[j] = content[pointer++];
			list.add(new AttributeInfo(name, length, value));
			LogUtils.printHex("attribute name: " + name + " length: " + length, value);
		}

		return list;
	}
	public static int getInteger(byte[] content) {
		return Byte.toUnsignedInt(content[pointer++]) << 24 | Byte.toUnsignedInt(content[pointer++]) << 16 |
				Byte.toUnsignedInt(content[pointer++]) << 8 | Byte.toUnsignedInt(content[pointer++]);
	}
	public static short getShort(byte[] content) {
		var x = ((Integer) (
					Byte.toUnsignedInt(content[pointer++]) << 8 | 
					Byte.toUnsignedInt(content[pointer++])
				)).shortValue();
		LogUtils.printHex("short: "+x, new byte[] {content[pointer-2], content[pointer-1]});
		return x;
	}

	public static Object poolConvert(byte[] content) {
		Object value = null;
		var size = content[pointer++];
		if (size == 3) { // INTEGER
			value = getInteger(content);
		} else if (size == 4) { // FLOAT
			value = ((Integer) getInteger(content)).floatValue();
		} else if (size == 5) { // LONG
			value = Long.valueOf(content[pointer++]<<56 | content[pointer++]<<48 | 
					content[pointer++]<<40 | content[pointer++]<<32 | content[pointer++]<<24 | 
					content[pointer++]<<16 | content[pointer++]<<8 | content[pointer++]);
		} else if (size == 6) { // DOUBLE
			value = (Long.valueOf((content[pointer++]<<56 | content[pointer++]<<48 |
					content[pointer++]<<40 | content[pointer++]<<32 | content[pointer++]<<24 |
					content[pointer++]<<16 | content[pointer++]<<8 | content[pointer++]))).doubleValue();
		} else if (size == 7) { // CLASS
			value = getShort(content);
			System.out.println("Class: "+value);
		} else if (size == 8) { // STRING
			value = getShort(content);
			System.out.println("String: "+value);
		} else if (size == 9) { // FIELDREF
			value = getInteger(content);
			System.out.println("FieldRef: "+value);
		} else if (size == 10) { // METHODREF
			value = getInteger(content);
			System.out.println("MethodRef: "+value);
		} else if (size == 11) { // INTERFACEMETHODREF
			value = getInteger(content);
			System.out.println("InterfaceMethodRef: "+value);
		} else if (size == 12) { // NAMEANDTYPE
			value = getInteger(content);
			System.out.println("NameAndType: "+value);
		} else if (size == 15) { // METHODHANDLE
			value = content[pointer++]<<16 | content[pointer++]<<8 | content[pointer++];
			System.out.println("MethodHandle: "+value);
		} else if (size == 16) { // METHODTYPE
			value = getShort(content);
			System.out.println("MethodType: "+value);
		} else if (size == 17) { // DYNAMIC
			value = getInteger(content);
			System.out.println("Dynamic: "+value);
		} else if (size == 18) { // INVOKEDYNAMIC
			value = getInteger(content);
			System.out.println("InvokeDynamic: "+value);
		} else if (size == 19) { // MODULE
			value = getShort(content);
			System.out.println("Module: "+value);
		} else if (size == 20) { // PACKAGE
			value = getShort(content);
			System.out.println("Package: "+value);
		} else if (size == 1) {
			var length = getShort(content);
			var list = new byte[length];
			for (int j = 0; j < length; j++) list[j] = content[pointer++];

			value = new String(list, StandardCharsets.UTF_8);
			System.out.println("UTF8: `"+value+"`");
		}
		return value;
	}

}
