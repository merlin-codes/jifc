import java.util.ArrayList;
import java.util.List;

/**
 * ByteCodeConvertor
 * convert code bytes array to instruction or other way
 */
public class ByteCodeConvertor {

	public void instructionConvert(byte[] content, List<Object> constants) {
		var max_stack = getShort(content, 0);
		var max_locals = getShort(content, 2);
		var code_length = getInteger(content, 4);

		System.out.println("max_stack: " + max_stack + 
				" max_locals: " + max_locals + 
				" code_length: " + code_length);

		int pointer = 8;
		while (pointer < (8 + code_length)) {
			pointer += interInstruction(content, constants, pointer);
			System.out.println("pointer: " + pointer + " " + (8 + code_length));
		}
		var exception_table_length = getShort(content, pointer);
		for (int i = 0; i < exception_table_length; i++) {
			var start_pc = getShort(content, pointer);
			var end_pc = getShort(content, pointer);
			var handler_pc = getShort(content, pointer);
			var catch_type = getShort(content, pointer);
			System.out.println("start_pc: " + start_pc +
					" end_pc: " + end_pc +
					" handler_pc: " + handler_pc +
					" catch_type: " + catch_type);
		}
		System.out.println("before parsing attributes");
		// var info = ParseClassFile.attributeConvert(content);

		System.out.println("end");
	}
	public short getShort(byte[] content, int pointer) {
		LogUtils.printHex("short("+pointer+":"+(pointer+1)+"): ", new byte[] {
				content[pointer], content[pointer+1]});

		return Integer.valueOf(
				Byte.toUnsignedInt(content[pointer++]) << 8 | 
				Byte.toUnsignedInt(content[pointer++])
		).shortValue();
	}
	public int getInteger(byte[] content, int pointer) {
		LogUtils.printHex("int("+pointer+":"+(pointer+3)+"): ", new byte[] {
				content[pointer], content[pointer+1], content[pointer+2], content[pointer+3]});

		return  Byte.toUnsignedInt(content[pointer++]) << 24 | 
				Byte.toUnsignedInt(content[pointer++]) << 16 |
				Byte.toUnsignedInt(content[pointer++]) << 8 | 
				Byte.toUnsignedInt(content[pointer++]);
	}
	private static Stack stack;
	public ByteCodeConvertor() { stack = new Stack(); }

	public class Stack {
		private ArrayList<Object> stackObjects = new ArrayList<>();
		private ArrayList<Object> localVariables = new ArrayList<>();

		public Stack() { }

		public void pushToStack(Object obj, int index) { stackObjects.add(index, obj); }
		public void pushToStack(Object obj) { stackObjects.add(obj); }
		public Object getStack(int index) { return stackObjects.get(index); }
		public Object getStackLast() { return stackObjects.get(stackObjects.size()-1); }

		public void addLocal(Object obj, int index) { localVariables.add(index, obj); }
		public void addLocal(Object obj) { localVariables.add(obj); }
		public Object getLocal(int index) { return localVariables.get(index); }
		public Object getLocalLast() { return localVariables.get(localVariables.size()-1); }

	}

	public int interInstruction(byte[] content, List<Object> constants, int pointer) {
		var byter = Byte.toUnsignedInt(content[pointer]);
		var old_byter = content[pointer++];
		var inc = 1;
		var x = "";
		var type = EnumsUtils.InstructionByteCodeEnum.valueOf(byter);
		var skip = false;

		short cont;

		switch (type) {
			case aload_0:
				x = "aload_0";
			break; case invokespecial:
				cont = getShort(content, pointer);
				x = "invokespecial "+constants.get(cont);
				inc += 2;
			break; case return_:
				x = "return";
			break; case getstatic:
				cont = getShort(content, pointer);
				x = "getstatic "+constants.get(cont -1);
				inc += 2;
			break; case ldc:
				cont = content[pointer++];
				stack.pushToStack(constants.get((Short) constants.get(cont - 1) - 1));
				x = "ldc "+stack.getStack(stack.stackObjects.size()-1);
				inc += 1;
			break; case invokevirtual:
				cont = getShort(content, pointer);
				System.out.println("\t INVOKEVIRTUAL> " + cont);
				System.out.println("INVOKE VIRTUAL - FIX: " + constants.get(cont) + ", " + constants.get(cont-1) + " " + cont + " " + (cont - 1));
				x = "invokevirtual "+constants.get((Short) constants.get(cont) - 1);
				inc += 2;
			break; default:
				x = "Instruction not Implemented: " + type.text();
				break;
		}
		/*

		if (type == EnumsUtils.InstructionByteCodeEnum.invokedynamic) {
			cont = getShort(content, pointer);
			x = "invokedynamic "+constants.get(cont);
			inc += 2;
		} else if (type == EnumsUtils.InstructionByteCodeEnum.invokespecial) {
			cont = getShort(content, pointer);
			x = "invokespecial "+constants.get(cont);
			inc += 2;
		} else if (type == EnumsUtils.InstructionByteCodeEnum.aconst_null) {
			stack.pushToStack(null);
			// skip = true;
			x = "push a null reference onto the stack";
		} else if (type == EnumsUtils.InstructionByteCodeEnum.nop) {
			// skip = true;
			x = "nop";
		} else if (type == EnumsUtils.InstructionByteCodeEnum.iconst_m1) {
			stack.pushToStack(-1);
			x = "load the -1 onto the stack";
		} else if (type.text().startsWith("iconst_")) {
			stack.pushToStack(Integer.parseInt(type.text().substring(7)));
			x = "iconst "+(stack.getStackLast()); // stackObjects.get(stack.stackObjects.size()-1);
		} else if (type.text().startsWith("lconst_")) {
			stack.pushToStack(0L);
			x = "lconst "+stack.getStackLast();
		} else if (type.text().startsWith("aload_")) {
			stack.pushToStack(Integer.parseInt(type.text().substring(6)));
			x = "aload_"+stack.getStackLast();
		} else {
			x = type.text()+" "+content[pointer++];
		}
		*/
		if (!skip) LogUtils.printHex(x, new byte[] { old_byter });
		return inc;
	}
	public static int switcher(int content) {
		var val = EnumsUtils.InstructionByteCodeEnum.valueOf(content);
		// System.out.print(val.text()+" ");
		return val.value;
	}
}
