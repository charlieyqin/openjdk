/*
* Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
*
* This code is free software; you can redistribute it and/or modify it
* under the terms of the GNU General Public License version 2 only, as
* published by the Free Software Foundation.
*
* This code is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
* FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
* version 2 for more details (a copy is included in the LICENSE file that
* accompanied this code).
*
* You should have received a copy of the GNU General Public License version
* 2 along with this work; if not, write to the Free Software Foundation,
* Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
*
* Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
* or visit www.oracle.com if you need additional information or have any
* questions.
*/

/*
 * @test TestVerifyBeforeAndAfterGCFlags
 * @key gc
 * @bug 8000831
 * @summary Runs an simple application (GarbageProducer) with various
         combinations of -XX:{+|-}Verify{After|Before}GC flags and checks that
         output contain or doesn't contain expected patterns
 * @modules java.management
 * @library /testlibrary
 * @run driver TestVerifyBeforeAndAfterGCFlags
 */

import java.util.ArrayList;
import java.util.Collections;

import com.oracle.java.testlibrary.Utils;
import com.oracle.java.testlibrary.OutputAnalyzer;
import com.oracle.java.testlibrary.ProcessTools;

public class TestVerifyBeforeAndAfterGCFlags {

    // VerifyBeforeGC:[Verifying threads heap tenured eden syms strs zone dict metaspace chunks hand C-heap code cache ]
    public static final String VERIFY_BEFORE_GC_PATTERN = "VerifyBeforeGC:\\[Verifying\\s+([^]\\s]+\\s+)+\\]";
    // VerifyBeforeGC: VerifyBeforeGC: VerifyBeforeGC:
    public static final String VERIFY_BEFORE_GC_CORRUPTED_PATTERN = "VerifyBeforeGC:(?!\\[Verifying[^]]+\\])";

    // VerifyAfterGC:[Verifying threads heap tenured eden syms strs zone dict metaspace chunks hand C-heap code cache ]
    public static final String VERIFY_AFTER_GC_PATTERN = "VerifyAfterGC:\\[Verifying\\s+([^]\\s]+\\s+)+\\]";
    // VerifyAfterGC: VerifyAfterGC: VerifyAfterGC:
    public static final String VERIFY_AFTER_GC_CORRUPTED_PATTERN = "VerifyAfterGC:(?!\\[Verifying[^]]+\\])";

    public static void main(String args[]) throws Exception {
        String[] filteredOpts = Utils.getFilteredTestJavaOpts(
                                    new String[] { "-Xloggc:",
                                                   "-XX:+UseGCLogFileRotation",
                                                   "-XX:-DisplayVMOutput",
                                                   "VerifyBeforeGC",
                                                   "VerifyAfterGC" });
        testVerifyFlags(false, false, filteredOpts);
        testVerifyFlags(true,  true,  filteredOpts);
        testVerifyFlags(true,  false, filteredOpts);
        testVerifyFlags(false, true,  filteredOpts);
    }

    public static void testVerifyFlags(boolean verifyBeforeGC,
                                       boolean verifyAfterGC,
                                       String[] opts) throws Exception {
        ArrayList<String> vmOpts = new ArrayList<>();
        if (opts != null && (opts.length > 0)) {
            Collections.addAll(vmOpts, opts);
        }

        Collections.addAll(vmOpts, new String[] {
                                       "-Xmx5m",
                                       "-Xms5m",
                                       "-Xmn3m",
                                       "-XX:+UnlockDiagnosticVMOptions",
                                       (verifyBeforeGC ? "-XX:+VerifyBeforeGC"
                                                       : "-XX:-VerifyBeforeGC"),
                                       (verifyAfterGC ? "-XX:+VerifyAfterGC"
                                                      : "-XX:-VerifyAfterGC"),
                                       GarbageProducer.class.getName() });
        ProcessBuilder procBuilder =
            ProcessTools.createJavaProcessBuilder(vmOpts.toArray(
                                                   new String[vmOpts.size()]));
        OutputAnalyzer analyzer = new OutputAnalyzer(procBuilder.start());

        analyzer.shouldHaveExitValue(0);
        analyzer.shouldNotMatch(VERIFY_BEFORE_GC_CORRUPTED_PATTERN);
        analyzer.shouldNotMatch(VERIFY_AFTER_GC_CORRUPTED_PATTERN);

        if (verifyBeforeGC) {
            analyzer.shouldMatch(VERIFY_BEFORE_GC_PATTERN);
        } else {
            analyzer.shouldNotMatch(VERIFY_BEFORE_GC_PATTERN);
        }

        if (verifyAfterGC) {
            analyzer.shouldMatch(VERIFY_AFTER_GC_PATTERN);
        } else {
            analyzer.shouldNotMatch(VERIFY_AFTER_GC_PATTERN);
        }
    }

    public static class GarbageProducer {
        static long[][] garbage = new long[10][];

        public static void main(String args[]) {
            int j = 0;
            for(int i = 0; i<1000; i++) {
                garbage[j] = new long[10000];
                j = (j+1)%garbage.length;
            }
        }
    }
}
