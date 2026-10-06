import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.baksmali.*;

public class DexInspect {
    public static void main(String[] args) throws Exception {
        var container = DexFileFactory.loadDexContainer(new File(args[0]), Opcodes.forApi(37));
        String filter = args[1];
        for (String entry : container.getDexEntryNames()) {
            var dex = container.getEntry(entry).getDexFile();
            List<String> selected = new ArrayList<>();
            for (ClassDef c : dex.getClasses()) {
                if (!c.getType().matches(filter)) continue;
                selected.add(c.getType());
                System.out.println("CLASS " + c.getType() + " EXTENDS " + c.getSuperclass());
                for (Field f : c.getFields()) System.out.println("  FIELD " + f.getName() + ":" + f.getType());
                for (Method m : c.getMethods()) {
                    StringBuilder sig = new StringBuilder(m.getName()).append('(');
                    for (CharSequence p : m.getParameterTypes()) sig.append(p);
                    sig.append(')').append(m.getReturnType());
                    System.out.println("  METHOD " + sig);
                }
            }
            if (args.length > 2 && !selected.isEmpty()) {
                BaksmaliOptions options = new BaksmaliOptions();
                options.apiLevel = 37;
                Baksmali.disassembleDexFile(dex, new File(args[2]), 4, options, selected);
            }
        }
    }
}
