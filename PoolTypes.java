/**
 * PoolTypes
 */
public class PoolTypes {
	public static enum ConstantPoolType {
		CONSTANT_Utf8(1),
		CONSTANT_Integer(3),
		CONSTANT_Float(4),
		CONSTANT_Long(5),
		CONSTANT_Double(6),
		CONSTANT_Class(7),
		CONSTANT_String(8),
		CONSTANT_Fieldref(9),
		CONSTANT_Methodref(10),
		CONSTANT_InterfaceMethodref(11),
		CONSTANT_NameAndType(12),
		CONSTANT_MethodHandle(15),
		CONSTANT_MethodType(16),
		CONSTANT_Dynamic(17),
		CONSTANT_InvokeDynamic(18),
		CONSTANT_Module(19),
		CONSTANT_Package(20);

		private int value;
		private ConstantPoolType(int value) {
			this.value = value;
		}
	}
	public static enum FieldTableType {
		ACC_PUBLIC(0x0001),
		ACC_PRIVATE(0x0002),
		ACC_PROTECTED(0x0004),
		ACC_STATIC(0x0008),
		ACC_FINAL(0x0010),
		ACC_VOLATILE(0x0040),
		ACC_TRANSIENT(0x0080),
		ACC_SYNTHETIC(0x1000),
		ACC_ENUM(0x4000);
		private int value;
		private FieldTableType(int value) {
			this.value = value;
		}
	}
	public static enum MethodTableType {
		ACC_PUBLIC(0x0001),
		ACC_PRIVATE(0x0002),
		ACC_PROTECTED(0x0004),
		ACC_STATIC(0x0008),
		ACC_FINAL(0x0010),
		ACC_SYNCHRONIZED(0x0020),
		ACC_BRIDGE(0x0040),
		ACC_VARARGS(0x0080),
		ACC_NATIVE(0x0100),
		ACC_ABSTRACT(0x0400),
		ACC_STRICT(0x0800),
		ACC_SYNTHETIC(0x1000),
		ACC_ANNOTATION(0x2000),
		ACC_ENUM(0x4000);
		private int value;
		private MethodTableType(int value) {
			this.value = value;
		}
	}
}
