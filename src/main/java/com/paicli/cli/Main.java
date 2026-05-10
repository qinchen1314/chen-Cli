package com.paicli.cli;

import com.paicli.agent.Agent;
import com.paicli.agent.PlanExecuteAgent;
import com.paicli.agent.plan.JsonPlanner;
import com.paicli.agent.plan.PlanExecutor;
import com.paicli.llm.GLMClient;
import com.paicli.tool.ToolRegistry;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws IOException {
        GLMClient glmClient = new GLMClient();
        ToolRegistry tools = new ToolRegistry();
        Agent reactAgent = new Agent(glmClient, tools);
        JsonPlanner planner = new JsonPlanner(glmClient);
        PlanExecutor executor = new PlanExecutor(maxParallelism(), task -> reactAgent.run(task.description()));
        PlanExecuteAgent agent = new PlanExecuteAgent(reactAgent, planner, executor);

        if (args.length > 0) {
            System.out.println(agent.run(String.join(" ", args)));
            return;
        }

        runShell(agent, tools);
    }

    private static void runShell(PlanExecuteAgent agent, ToolRegistry tools) throws IOException {
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();
        Completer completer = new StringsCompleter("/exit", "/help", "/tool", "plan and execute", "quick");
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .highlighter(new PaiHighlighter())
                .variable(LineReader.HISTORY_FILE, historyFile())
                .build();

        terminal.writer().println("PaiCLI ready. Type /help or /exit.");
        terminal.writer().flush();
        while (true) {
            try {
                String line = reader.readLine("pai> ").trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.equals("/exit")) {
                    return;
                }
                if (line.equals("/help")) {
                    terminal.writer().println("Commands: /help, /exit, /tool <name> <input>");
                    terminal.writer().println("Tools: " + String.join(", ", tools.names()));
                    terminal.writer().flush();
                    continue;
                }
                terminal.writer().println(agent.run(line));
                terminal.writer().flush();
            } catch (UserInterruptException e) {
                terminal.writer().println("^C");
                terminal.writer().flush();
            } catch (EndOfFileException e) {
                return;
            } catch (RuntimeException e) {
                terminal.writer().println("ERROR: " + e.getMessage());
                terminal.writer().flush();
            }
        }
    }

    private static Path historyFile() {
        return Path.of(System.getProperty("user.home"), ".paicli_history");
    }

    private static int maxParallelism() {
        String value = System.getenv("PAICLI_MAX_PARALLELISM");
        if (value == null || value.isBlank()) {
            return 4;
        }
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return 4;
        }
    }

    private static class PaiHighlighter implements Highlighter {
        @Override
        public AttributedString highlight(LineReader reader, String buffer) {
            AttributedStringBuilder builder = new AttributedStringBuilder();
            if (buffer.startsWith("/")) {
                builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
            } else if (buffer.toLowerCase().contains("plan")) {
                builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
            } else {
                builder.style(AttributedStyle.DEFAULT);
            }
            return builder.append(buffer).toAttributedString();
        }

        @Override
        public void setErrorPattern(Pattern errorPattern) {
        }

        @Override
        public void setErrorIndex(int errorIndex) {
        }
    }
}
