# PaiCLI

PaiCLI 是一个 Java 17 + Maven 实现的 Agent CLI 示例项目，包含 ReAct 执行模式和 Plan-and-Execute 执行模式。

项目能力：

- ReAct Agent 基础循环
- GLM-5.1 API 客户端封装
- Plan-and-Execute 自动模式切换
- 任务分解、DAG 依赖管理、DFS 拓扑排序和循环依赖检测
- 基于线程池的无依赖任务并发执行
- JLine3 交互式命令行，支持历史记录、Tab 补全、行编辑和语法高亮
- JUnit 5 自测覆盖核心逻辑

## 环境要求

- JDK 17+
- Maven 3.8+
- Git
- GitHub SSH key，只有需要推送代码时才需要

检查环境：

```bash
java -version
mvn -version
git --version
```

## 从零开始

克隆仓库：

```bash
git clone git@github.com:qinchen1314/chen-Cli.git
cd chen-Cli
```

如果是在当前机器已有项目目录中运行：

```bash
cd C:\Users\16643\paicli
```

## 配置环境变量

复制示例配置：

```bash
copy .env.example .env
```

编辑 `.env`：

```env
GLM_API_KEY=replace-with-your-glm-api-key
GLM_BASE_URL=https://open.bigmodel.cn/api/paas/v4/chat/completions
GLM_MODEL=glm-5.1
PAICLI_MAX_PARALLELISM=4
```

说明：

- `.env` 是本地隐私文件，已被 `.gitignore` 忽略，不会提交到 Git。
- `.env.example` 是安全模板，可以提交。
- 如果只运行内置工具命令，例如 `/tool echo hello`，不需要真实 `GLM_API_KEY`。
- 如果运行普通自然语言请求或 Plan-and-Execute 请求，需要配置真实 `GLM_API_KEY`。

## 安装依赖并自测

运行完整测试：

```bash
mvn test
```

当前测试覆盖：

- 6 种任务类型
- 5 种任务状态流转
- DAG 双向依赖追踪
- DFS 拓扑排序
- 循环依赖检测
- Planner JSON 前向引用解析
- 并发执行无依赖任务
- 失败依赖阻断下游任务
- ReAct 和 Plan-and-Execute 模式切换

预期结果：

```text
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 运行项目

### 方式一：非交互式运行

运行内置工具，不需要 GLM key：

```powershell
mvn exec:java "-Dexec.args=/tool echo hello"
```

预期输出：

```text
hello
```

运行普通 Agent 请求，需要 GLM key：

```powershell
mvn exec:java "-Dexec.args=quick explain this project"
```

运行 Plan-and-Execute 请求，需要 GLM key：

```powershell
mvn exec:java "-Dexec.args=plan and execute adding a new tool with tests"
```

### 方式二：交互式 CLI

启动：

```bash
mvn exec:java
```

进入后可以输入：

```text
/help
/tool echo hello
quick explain this project
plan and execute adding a new tool with tests
/exit
```

CLI 支持：

- 上下箭头查看历史命令
- Tab 自动补全
- 行编辑
- 命令高亮
- Ctrl+C 中断当前输入
- Ctrl+D 退出

历史文件默认保存到：

```text
%USERPROFILE%\.paicli_history
```

该文件不会提交到 Git。

## 项目结构

```text
paicli/
├── pom.xml
├── .env.example
├── .gitignore
└── src/
    ├── main/java/com/paicli/
    │   ├── cli/Main.java
    │   ├── agent/Agent.java
    │   ├── agent/PlanExecuteAgent.java
    │   ├── agent/plan/
    │   │   ├── PlanTask.java
    │   │   ├── TaskType.java
    │   │   ├── TaskStatus.java
    │   │   ├── TaskGraph.java
    │   │   ├── TopologicalSorter.java
    │   │   ├── JsonPlanner.java
    │   │   └── PlanExecutor.java
    │   ├── llm/GLMClient.java
    │   └── tool/ToolRegistry.java
    └── test/java/com/paicli/
        ├── agent/PlanExecuteAgentTest.java
        └── agent/plan/
            ├── TaskGraphTest.java
            ├── JsonPlannerTest.java
            └── PlanExecutorTest.java
```

## 核心设计

### Task 模型

任务类型：

- `RESEARCH`
- `REASONING`
- `CODING`
- `TOOL_CALL`
- `VALIDATION`
- `RESPONSE`

任务状态：

- `PENDING`
- `READY`
- `RUNNING`
- `SUCCEEDED`
- `FAILED`

合法状态流转：

```text
PENDING -> READY
READY -> RUNNING
RUNNING -> SUCCEEDED
RUNNING -> FAILED
READY -> FAILED
PENDING -> FAILED
```

### DAG 与拓扑排序

`TaskGraph` 维护：

- `dependsOn`：当前任务依赖哪些任务
- `dependents`：哪些任务依赖当前任务

`TopologicalSorter` 使用 DFS 三色标记：

- `UNVISITED`
- `VISITING`
- `VISITED`

当 DFS 遇到 `VISITING` 节点时，说明存在循环依赖，会抛出 `CycleDetectedException`。

### 并发执行

`PlanExecutor` 使用有界线程池执行任务：

1. 先校验 DAG 无环。
2. 找到没有依赖的任务并发提交。
3. 一个任务成功后，减少下游任务的剩余依赖数。
4. 下游任务所有依赖完成后进入 `READY` 并提交执行。
5. 如果任务失败，下游任务会被标记为 `FAILED`。

### Planner JSON

`JsonPlanner` 要求 LLM 返回严格 JSON：

```json
{
  "tasks": [
    {
      "id": "T1",
      "type": "RESEARCH",
      "title": "Inspect project",
      "description": "Read current project structure.",
      "dependsOn": []
    }
  ]
}
```

解析时会校验：

- 任务数量
- 任务 ID 唯一性
- 任务类型合法性
- 依赖 ID 是否存在
- 是否自依赖
- 是否存在循环依赖

## 隐私安全

不会提交：

- `.env`
- `target/`
- `.paicli_history`
- IDE 本地文件
- 日志和临时文件

提交前建议检查：

```bash
git status --short --ignored
git diff --cached --name-only
```

## 常见问题

### `GLM_API_KEY is required`

说明当前命令需要调用 GLM API，但 `.env` 或环境变量中没有配置密钥。

解决：

```bash
copy .env.example .env
```

然后填写真实 `GLM_API_KEY`。

### 只想验证项目能运行

使用不需要 GLM key 的内置工具：

```powershell
mvn exec:java "-Dexec.args=/tool echo hello"
```

### 推送到 GitHub

确认远程地址：

```bash
git remote -v
```

推送：

```bash
git push -u origin main
```
