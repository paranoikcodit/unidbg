package com.github.unidbg.android;

import com.alibaba.fastjson.util.IOUtils;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.NestedRun;
import com.github.unidbg.arm.backend.DynarmicFactory;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.arm.context.EditableArm32RegisterContext;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.android.AndroidARMEmulator;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.struct.Dirent;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.unix.UnixSyscallHandler;
import com.github.unidbg.virtualmodule.android.SystemProperties;
import com.sun.jna.Pointer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Arrays;

public class AndroidTest extends AbstractJni {

    public static void main(String[] args) {
        AndroidTest test = new AndroidTest();
        test.test();
        test.destroy();
    }

    private void destroy() {
        IOUtils.close(emulator);
    }

    private final AndroidEmulator emulator;
    private final Module module;
    private final DvmClass cJniTest;

    private static class MyARMSyscallHandler extends ARM32SyscallHandler {
        private MyARMSyscallHandler(SvcMemory svcMemory) {
            super(svcMemory);
        }

        @Override
        protected int fork(Emulator<?> emulator) {
            return emulator.getPid();
        }
    }

    private AndroidTest() {
        final File executable = new File("unidbg-android/src/test/native/android/libs/armeabi-v7a/test");
        emulator = new AndroidARMEmulator(executable.getName(),
                new File("target/rootfs"),
                Arrays.asList(new DynarmicFactory(true), new Unicorn2Factory(true)), new String[0]) {
            @Override
            protected UnixSyscallHandler<AndroidFileIO> createSyscallHandler(SvcMemory svcMemory) {
                return new MyARMSyscallHandler(svcMemory);
            }
        };
        Memory memory = emulator.getMemory();
        emulator.getSyscallHandler().setVerbose(false);
        emulator.getSyscallHandler().setEnableThreadDispatcher(true);
        AndroidResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);
        new SystemProperties(emulator, null).register(memory);

        module = emulator.loadLibrary(executable, true);

        VM vm = emulator.createDalvikVM();
        vm.setVerbose(true);
        vm.setJni(this);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/native/android/libs/armeabi-v7a/libnative.so"), true);
        dm.callJNI_OnLoad(emulator);
        this.cJniTest = vm.resolveClass("com/github/unidbg/android/JniTest");

        {
            Pointer pointer = memory.allocateStack(0x100);
            System.out.println(new Dirent(pointer));
        }
    }

    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        if ("com/github/unidbg/android/AndroidTest-><init>()V".equals(signature)) {
            return dvmClass.newObject(null);
        }
        return super.newObject(vm, dvmClass, signature, varArg);
    }

    @Override
    public void setFloatField(BaseVM vm, DvmObject<?> dvmObject, String signature, float value) {
        if ("com/github/unidbg/android/AndroidTest->floatField:F".equals(signature)) {
            System.out.println("floatField value=" + value);
            return;
        }
        super.setFloatField(vm, dvmObject, signature, value);
    }

    @Override
    public void setDoubleField(BaseVM vm, DvmObject<?> dvmObject, String signature, double value) {
        if ("com/github/unidbg/android/AndroidTest->doubleField:D".equals(signature)) {
            System.out.println("doubleField value=" + value);
            return;
        }
        super.setDoubleField(vm, dvmObject, signature, value);
    }

    @Override
    public float callStaticFloatMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        if ("com/github/unidbg/android/AndroidTest->testStaticFloat(FD)F".equals(signature)) {
            float f1 = varArg.getFloatArg(0);
            double d2 = varArg.getDoubleArg(1);
            System.out.printf("callStaticFloatMethod f1=%s, d2=%s%n", f1, d2);
            return 0.0033942017F;
        }

        return super.callStaticFloatMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public double callStaticDoubleMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        if ("com/github/unidbg/android/AndroidTest->testStaticDouble(FD)D".equals(signature)) {
            return 0.0023942017D;
        }
        return super.callStaticDoubleMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public boolean getStaticBooleanField(BaseVM vm, DvmClass dvmClass, String signature) {
        if ("com/github/unidbg/android/AndroidTest->staticBooleanField:Z".equals(signature)) {
            return true;
        }

        return super.getStaticBooleanField(vm, dvmClass, signature);
    }

    @Override
    public void setStaticDoubleField(BaseVM vm, DvmClass dvmClass, String signature, double value) {
        if ("com/github/unidbg/android/AndroidTest->staticDoubleField:D".equals(signature)) {
            System.out.println("staticDoubleField value=" + value);
            return;
        }

        super.setStaticDoubleField(vm, dvmClass, signature, value);
    }

    @Override
    public void setStaticFloatField(BaseVM vm, DvmClass dvmClass, String signature, float value) {
        if ("com/github/unidbg/android/AndroidTest->staticFloatField:F".equals(signature)) {
            System.out.println("staticFloatField value=" + value);
            return;
        }

        super.setStaticFloatField(vm, dvmClass, signature, value);
    }

    @Override
    public long callStaticLongMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        if ("com/github/unidbg/android/JniTest->nestedRun(Ljava/lang/String;JID)J".equals(signature)) {
            { // fix call context, can't fix stack args.
                StringObject str = varArg.getObjectArg(0);
                long l1 = varArg.getLongArg(1);
                int i1 = varArg.getIntArg(2);
                double d1 = varArg.getDoubleArg(3);
                System.out.println("nestedRunInJava l1=0x" + Long.toHexString(l1 / 2) + ", i1=0x" + Integer.toHexString(i1 / 2) + ", d1=" + d1 / 2 + ", str=" + str.getValue());
                EditableArm32RegisterContext context = emulator.getContext();
                context.setR2(str.hashCode());
            }
            UnidbgPointer fun = dvmClass.findNativeFunction(emulator, "nestedRun(Ljava/lang/String;JID)J");
            throw NestedRun.runToFunction(UnidbgPointer.nativeValue(fun));
        }

        return super.callStaticLongMethod(vm, dvmClass, signature, varArg);
    }

    private void test() {
        cJniTest.callStaticJniMethod(emulator, "testJni(Ljava/lang/String;JIDZSFDBJF)V",
                getClass().getName(), 0x123456789abcdefL,
                0x789a, 0.12345D, true, 0x123, 0.456f, 0.789123D, (byte) 0x7f,
                0x89abcdefL, 0.123f);

        Logger.getLogger("com.github.unidbg.thread").setLevel(Level.INFO);
        Logger.getLogger("com.github.unidbg.linux.AndroidSyscallHandler").setLevel(Level.INFO);
        System.err.println("exit code: " + module.callEntry(emulator) + ", backend=" + emulator.getBackend());
    }

}
