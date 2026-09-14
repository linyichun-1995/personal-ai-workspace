# AI Personal Workspace UI Design System

> 版本：1.0  
> 适用技术栈：React + TypeScript + Vite + Tailwind CSS v4 + shadcn/ui  
> 文档语言：中文  
> 适用范围：Web 后台工作台、设计稿、组件库、主题系统和前端验收  
> 关键词：Minimal / Calm / Professional / AI-native / Productivity / High-density

---

## 1. 文档目标

本规范是 AI Personal Workspace 的唯一 UI 设计基线，用于保证 Dashboard、Project、Task、Note、File、Search、AI Chat、AI Knowledge、AI Agent、Integration、Settings 等模块在长期迭代中保持一致。

本规范重点解决：

- 设计与前端使用同一套语义变量，不在业务组件中散落硬编码颜色和尺寸。
- Light、Dark 与 Accent Color 相互解耦，新增主题或强调色时无需修改业务组件。
- 后台界面保持简洁，但不空洞；信息密度适中偏高，适合长时间工作。
- Sidebar 支持完整展开、紧凑折叠、移动端抽屉及多级菜单。
- 一级、二级及更深层级菜单均有明确的图标、缩进、选中和展开规则。
- AI 是产品能力和工作流入口，而不是大面积霓虹、渐变或机器人装饰。

### 1.1 优先级

规范冲突时按以下顺序处理：

1. 可访问性与可操作性。
2. 信息层级与任务效率。
3. 设计 Token 与组件一致性。
4. 响应式适配。
5. 视觉装饰。

### 1.2 非目标

本文不包含业务逻辑、接口设计、路由实现、状态管理或具体 React 页面代码。CSS 示例只用于定义主题变量和 Tailwind 映射，不代表业务实现。

---

## 2. 产品视觉方向

### 2.1 核心气质

- **中性克制**：以灰阶、留白、排版和轻边框构建层级。
- **专业安静**：长时间阅读和操作不疲劳，不使用刺眼纯白或大面积纯黑。
- **AI-native**：AI 入口清晰、随上下文可用；视觉上仅通过小范围光晕、动态图标或生成状态表达智能感。
- **高效紧凑**：默认正文 14px，控件高度 36px，表格和列表支持 Compact 密度。
- **细节精致**：悬停、聚焦、展开、加载均有统一反馈，动画短而自然。

### 2.2 AI 视觉语言

AI 视觉只允许出现在以下场景：

- Ask AI、AI Chat、AI Agent、生成、总结、语义搜索等明确 AI 能力入口。
- AI 正在思考、调用工具、生成完成或需要用户确认的状态。
- 空状态中的轻量品牌图形。

表达方式：

- 使用 `Sparkles`、`WandSparkles`、`Bot`、`BrainCircuit` 等 Lucide 线性图标。
- 默认使用当前 Accent Color；多色效果仅限 1px 边缘光或低透明度径向渐变。
- 光晕透明度不得高于 `--opacity-12`，模糊不超过 `--blur-xl`。
- AI 内容区仍使用普通文本、列表、引用和代码块，不使用巨大对话气泡。

禁止：

- 大面积紫蓝渐变背景。
- 霓虹边框、持续闪烁、粒子雨、3D 机器人。
- 把所有 Primary 操作都标记成 AI。

### 2.3 参考原则

可以吸收 Linear、Notion、OpenAI、Raycast、Arc、Vercel 的克制、排版和交互思路，但不得复制其品牌、布局细节或专有视觉资产。

---

## 3. Token 架构

### 3.1 四层模型

| 层级 | 示例 | 用途 |
|---|---|---|
| Primitive | `--gray-950`、`--indigo-600` | 原始色阶，只在主题文件中使用 |
| Semantic | `--background`、`--primary`、`--border` | 表达界面含义，组件必须优先使用 |
| Component | `--sidebar-active`、`--dialog-shadow` | 组件独有语义，避免重复组合 |
| Context | `--page-padding-x`、`--control-height` | 布局、密度和场景变量 |

业务组件不得直接使用 Primitive Token。Primitive 只负责生成 Semantic Token；Semantic Token 再供组件消费。

### 3.2 命名规则

- 背景使用名词：`--background`、`--surface-raised`。
- 内容色以 `-foreground` 结尾：`--card-foreground`。
- 边框以 `-border` 结尾，聚焦环使用 `-ring`。
- 状态必须成对：`--success` 与 `--success-foreground`。
- 不用具体色名命名业务用途，如 `--blue-button`、`--gray-text`。
- 不把 Dark 值写进变量名，如 `--dark-card`。
- 数值尺寸使用稳定刻度；语义尺寸使用用途名称。

### 3.3 颜色格式

颜色统一使用 OKLCH，便于保持不同主题中的感知亮度和对比度。透明效果使用 `color-mix(in oklch, token percentage, transparent)`，不另外硬编码 RGBA。

---

## 4. 完整 CSS Custom Properties

以下变量是设计系统的默认契约。项目可以拆分为多个样式文件，但变量名称和语义必须保持一致。

### 4.1 基础尺寸、排版、动效与布局变量

```css
:root {
  /* Font family */
  --font-sans: "Inter", "Geist", "PingFang SC", "Microsoft YaHei",
    "Noto Sans CJK SC", system-ui, -apple-system, BlinkMacSystemFont,
    "Segoe UI", sans-serif;
  --font-mono: "JetBrains Mono", "SFMono-Regular", Consolas,
    "Liberation Mono", monospace;

  /* Font size */
  --font-size-2xs: 0.6875rem; /* 11px */
  --font-size-xs: 0.75rem;    /* 12px */
  --font-size-sm: 0.8125rem;  /* 13px */
  --font-size-md: 0.875rem;   /* 14px, default */
  --font-size-lg: 1rem;       /* 16px */
  --font-size-xl: 1.125rem;   /* 18px */
  --font-size-2xl: 1.25rem;   /* 20px */
  --font-size-3xl: 1.5rem;    /* 24px */
  --font-size-4xl: 1.875rem;  /* 30px, exceptional */

  /* Line height */
  --line-height-none: 1;
  --line-height-tight: 1.25;
  --line-height-snug: 1.375;
  --line-height-normal: 1.5;
  --line-height-relaxed: 1.625;
  --line-height-loose: 1.75;

  /* Font weight */
  --font-weight-regular: 400;
  --font-weight-medium: 500;
  --font-weight-semibold: 600;
  --font-weight-bold: 700;

  /* Letter spacing */
  --tracking-tight: -0.015em;
  --tracking-normal: 0;
  --tracking-wide: 0.02em;

  /* Spacing */
  --space-0: 0;
  --space-px: 1px;
  --space-0-5: 0.125rem;
  --space-1: 0.25rem;
  --space-1-5: 0.375rem;
  --space-2: 0.5rem;
  --space-2-5: 0.625rem;
  --space-3: 0.75rem;
  --space-3-5: 0.875rem;
  --space-4: 1rem;
  --space-5: 1.25rem;
  --space-6: 1.5rem;
  --space-8: 2rem;
  --space-10: 2.5rem;
  --space-12: 3rem;
  --space-16: 4rem;
  --space-20: 5rem;
  --space-24: 6rem;

  /* Radius */
  --radius-none: 0;
  --radius-xs: 0.25rem;
  --radius-sm: 0.375rem;
  --radius-md: 0.5rem;
  --radius-lg: 0.75rem;
  --radius-xl: 1rem;
  --radius-full: 9999px;
  --radius: var(--radius-md); /* shadcn base */

  /* Shadow: neutral, low-noise */
  --shadow-none: 0 0 #0000;
  --shadow-xs: 0 1px 2px 0 rgb(0 0 0 / 0.04);
  --shadow-sm: 0 1px 3px rgb(0 0 0 / 0.06), 0 1px 2px rgb(0 0 0 / 0.03);
  --shadow-md: 0 8px 24px rgb(0 0 0 / 0.08), 0 2px 8px rgb(0 0 0 / 0.04);
  --shadow-lg: 0 16px 40px rgb(0 0 0 / 0.12), 0 4px 12px rgb(0 0 0 / 0.06);
  --shadow-dialog: 0 24px 72px rgb(0 0 0 / 0.18), 0 8px 24px rgb(0 0 0 / 0.08);
  --shadow-focus: 0 0 0 3px color-mix(in oklch, var(--ring) 22%, transparent);

  /* Z-index */
  --z-base: 0;
  --z-sticky: 20;
  --z-header: 30;
  --z-sidebar: 40;
  --z-dropdown: 50;
  --z-popover: 60;
  --z-overlay: 70;
  --z-dialog: 80;
  --z-toast: 90;
  --z-command: 100;
  --z-tooltip: 110;

  /* Breakpoint */
  --breakpoint-xs: 30rem;  /* 480px */
  --breakpoint-sm: 40rem;  /* 640px */
  --breakpoint-md: 48rem;  /* 768px */
  --breakpoint-lg: 64rem;  /* 1024px */
  --breakpoint-xl: 80rem;  /* 1280px */
  --breakpoint-2xl: 96rem; /* 1536px */
  --breakpoint-3xl: 120rem;/* 1920px */

  /* Container */
  --container-xs: 30rem;
  --container-sm: 40rem;
  --container-md: 48rem;
  --container-lg: 64rem;
  --container-xl: 80rem;
  --container-2xl: 90rem;
  --container-reading: 52rem;
  --container-chat: 62rem;
  --container-workspace: 100rem;

  /* Duration */
  --duration-instant: 0ms;
  --duration-fast: 120ms;
  --duration-normal: 180ms;
  --duration-slow: 240ms;
  --duration-emphasis: 320ms;

  /* Easing */
  --ease-linear: linear;
  --ease-standard: cubic-bezier(0.2, 0, 0, 1);
  --ease-enter: cubic-bezier(0, 0, 0.2, 1);
  --ease-exit: cubic-bezier(0.4, 0, 1, 1);
  --ease-spring-soft: cubic-bezier(0.22, 1, 0.36, 1);

  /* Opacity */
  --opacity-0: 0;
  --opacity-4: 0.04;
  --opacity-8: 0.08;
  --opacity-12: 0.12;
  --opacity-20: 0.2;
  --opacity-40: 0.4;
  --opacity-60: 0.6;
  --opacity-80: 0.8;
  --opacity-100: 1;
  --opacity-disabled: 0.5;
  --opacity-placeholder: 0.62;
  --opacity-overlay: 0.48;

  /* Blur */
  --blur-none: 0;
  --blur-sm: 4px;
  --blur-md: 8px;
  --blur-lg: 12px;
  --blur-xl: 20px;
  --blur-2xl: 32px;

  /* App shell */
  --sidebar-width: 15rem;             /* 240px */
  --sidebar-width-collapsed: 4rem;    /* 64px */
  --sidebar-item-height: 2.25rem;     /* 36px */
  --sidebar-icon-size: 1.125rem;      /* 18px */
  --sidebar-sub-icon-size: 1rem;      /* 16px */
  --sidebar-indent-step: 1rem;
  --sidebar-padding-x: 0.625rem;
  --sidebar-panel-gap: 0.25rem;
  --header-height: 3.5rem;            /* 56px */
  --mobile-header-height: 3.25rem;    /* 52px */
  --ai-panel-width: 25rem;            /* 400px */
  --ai-panel-width-min: 22.5rem;      /* 360px */
  --ai-panel-width-max: 27.5rem;      /* 440px */
  --inspector-width: 22rem;
  --content-width-default: 80rem;     /* 1280px */
  --content-width-wide: 100rem;       /* 1600px */
  --content-width-reading: 52rem;     /* 832px */
  --content-width-chat: 62rem;        /* 992px */
  --page-padding-x: 2rem;
  --page-padding-y: 1.5rem;
  --section-gap: 2rem;
  --grid-gap: 1rem;

  /* Controls and density; comfortable is default */
  --control-height-sm: 2rem;
  --control-height-md: 2.25rem;
  --control-height-lg: 2.5rem;
  --control-padding-x: 0.75rem;
  --control-gap: 0.5rem;
  --row-height: 2.75rem;
  --table-cell-padding-x: 0.75rem;
  --table-cell-padding-y: 0.625rem;
  --card-padding: 1.25rem;
  --form-gap: 1rem;
}
```

### 4.2 Light 默认主题

```css
:root,
[data-theme="light"] {
  color-scheme: light;

  /* Neutral primitives */
  --gray-0: oklch(1 0 0);
  --gray-25: oklch(0.992 0.002 255);
  --gray-50: oklch(0.982 0.003 255);
  --gray-100: oklch(0.96 0.005 255);
  --gray-200: oklch(0.918 0.008 255);
  --gray-300: oklch(0.855 0.012 255);
  --gray-400: oklch(0.69 0.018 255);
  --gray-500: oklch(0.55 0.02 255);
  --gray-600: oklch(0.445 0.02 255);
  --gray-700: oklch(0.36 0.018 255);
  --gray-800: oklch(0.27 0.016 255);
  --gray-900: oklch(0.205 0.014 255);
  --gray-950: oklch(0.145 0.012 255);

  /* Base */
  --background: oklch(0.985 0.003 255);
  --foreground: oklch(0.205 0.014 255);
  --surface: oklch(0.998 0.001 255);
  --surface-foreground: var(--foreground);
  --surface-subtle: oklch(0.972 0.004 255);
  --surface-raised: oklch(1 0 0);
  --surface-sunken: oklch(0.962 0.006 255);

  /* Card and floating layer */
  --card: oklch(0.998 0.001 255);
  --card-foreground: var(--foreground);
  --card-hover: oklch(0.989 0.003 255);
  --popover: oklch(1 0 0);
  --popover-foreground: var(--foreground);
  --dialog: oklch(1 0 0);
  --dialog-foreground: var(--foreground);
  --overlay: oklch(0.16 0.01 255 / 0.48);

  /* Brand/primary is assigned by data-accent */
  --brand: oklch(0.56 0.21 272);
  --brand-hover: oklch(0.51 0.22 272);
  --brand-active: oklch(0.47 0.20 272);
  --brand-subtle: oklch(0.95 0.03 272);
  --brand-muted: oklch(0.9 0.06 272);
  --brand-foreground: oklch(0.99 0.005 272);
  --primary: var(--brand);
  --primary-foreground: var(--brand-foreground);
  --primary-hover: var(--brand-hover);
  --primary-active: var(--brand-active);
  --primary-subtle: var(--brand-subtle);

  /* shadcn semantic surfaces */
  --secondary: oklch(0.952 0.006 255);
  --secondary-foreground: oklch(0.29 0.016 255);
  --secondary-hover: oklch(0.928 0.009 255);
  --muted: oklch(0.958 0.005 255);
  --muted-foreground: oklch(0.505 0.02 255);
  --accent: oklch(0.944 0.012 272);
  --accent-foreground: oklch(0.28 0.055 272);
  --accent-hover: oklch(0.918 0.02 272);

  /* Feedback */
  --destructive: oklch(0.57 0.215 27);
  --destructive-foreground: oklch(0.99 0.005 27);
  --destructive-subtle: oklch(0.96 0.035 27);
  --success: oklch(0.54 0.15 150);
  --success-foreground: oklch(0.99 0.005 150);
  --success-subtle: oklch(0.96 0.035 150);
  --warning: oklch(0.69 0.15 75);
  --warning-foreground: oklch(0.25 0.055 75);
  --warning-subtle: oklch(0.97 0.045 82);
  --info: oklch(0.57 0.17 245);
  --info-foreground: oklch(0.99 0.005 245);
  --info-subtle: oklch(0.96 0.035 245);

  /* Lines and focus */
  --border: oklch(0.902 0.009 255);
  --border-subtle: oklch(0.935 0.006 255);
  --border-strong: oklch(0.82 0.014 255);
  --input: oklch(0.89 0.01 255);
  --input-background: oklch(0.998 0.001 255);
  --ring: var(--brand);
  --focus-foreground: var(--foreground);

  /* Selection and code */
  --selection: color-mix(in oklch, var(--brand) 18%, transparent);
  --selection-foreground: var(--foreground);
  --code: oklch(0.955 0.006 255);
  --code-foreground: oklch(0.27 0.016 255);

  /* Sidebar */
  --sidebar: oklch(0.972 0.004 255);
  --sidebar-foreground: oklch(0.29 0.016 255);
  --sidebar-muted: oklch(0.952 0.006 255);
  --sidebar-muted-foreground: oklch(0.54 0.019 255);
  --sidebar-hover: oklch(0.94 0.008 255);
  --sidebar-active: color-mix(in oklch, var(--brand) 11%, oklch(0.975 0.004 255));
  --sidebar-active-foreground: oklch(0.27 0.06 272);
  --sidebar-primary: var(--brand);
  --sidebar-primary-foreground: var(--brand-foreground);
  --sidebar-accent: var(--sidebar-hover);
  --sidebar-accent-foreground: var(--sidebar-foreground);
  --sidebar-border: oklch(0.91 0.008 255);
  --sidebar-ring: var(--ring);
  --sidebar-section: oklch(0.56 0.018 255);
  --sidebar-icon: oklch(0.47 0.02 255);
  --sidebar-icon-active: var(--brand);

  /* Charts */
  --chart-1: oklch(0.58 0.19 272);
  --chart-2: oklch(0.61 0.14 185);
  --chart-3: oklch(0.65 0.16 145);
  --chart-4: oklch(0.72 0.15 82);
  --chart-5: oklch(0.63 0.18 25);
  --chart-6: oklch(0.61 0.17 320);
  --chart-7: oklch(0.62 0.12 225);
  --chart-8: oklch(0.56 0.04 255);
  --chart-grid: oklch(0.91 0.008 255);
  --chart-axis: oklch(0.54 0.019 255);
  --chart-tooltip: var(--popover);
  --chart-tooltip-foreground: var(--popover-foreground);
}
```

### 4.3 Dark 默认主题

```css
.dark,
[data-theme="dark"] {
  color-scheme: dark;

  /* Neutral primitives */
  --gray-0: oklch(0.985 0.002 255);
  --gray-25: oklch(0.94 0.004 255);
  --gray-50: oklch(0.88 0.006 255);
  --gray-100: oklch(0.79 0.009 255);
  --gray-200: oklch(0.69 0.012 255);
  --gray-300: oklch(0.59 0.014 255);
  --gray-400: oklch(0.5 0.015 255);
  --gray-500: oklch(0.42 0.014 255);
  --gray-600: oklch(0.35 0.013 255);
  --gray-700: oklch(0.29 0.012 255);
  --gray-800: oklch(0.235 0.011 255);
  --gray-900: oklch(0.185 0.01 255);
  --gray-950: oklch(0.145 0.009 255);

  /* Base: avoid pure black */
  --background: oklch(0.17 0.01 255);
  --foreground: oklch(0.93 0.006 255);
  --surface: oklch(0.195 0.011 255);
  --surface-foreground: var(--foreground);
  --surface-subtle: oklch(0.185 0.01 255);
  --surface-raised: oklch(0.225 0.012 255);
  --surface-sunken: oklch(0.145 0.009 255);

  /* Card and floating layer */
  --card: oklch(0.205 0.011 255);
  --card-foreground: var(--foreground);
  --card-hover: oklch(0.235 0.013 255);
  --popover: oklch(0.235 0.013 255);
  --popover-foreground: var(--foreground);
  --dialog: oklch(0.225 0.012 255);
  --dialog-foreground: var(--foreground);
  --overlay: oklch(0.08 0.005 255 / 0.68);

  /* Brand/primary is assigned by data-accent */
  --brand: oklch(0.7 0.17 272);
  --brand-hover: oklch(0.75 0.16 272);
  --brand-active: oklch(0.64 0.18 272);
  --brand-subtle: oklch(0.27 0.055 272);
  --brand-muted: oklch(0.34 0.075 272);
  --brand-foreground: oklch(0.17 0.025 272);
  --primary: var(--brand);
  --primary-foreground: var(--brand-foreground);
  --primary-hover: var(--brand-hover);
  --primary-active: var(--brand-active);
  --primary-subtle: var(--brand-subtle);

  /* shadcn semantic surfaces */
  --secondary: oklch(0.255 0.012 255);
  --secondary-foreground: oklch(0.91 0.007 255);
  --secondary-hover: oklch(0.292 0.014 255);
  --muted: oklch(0.245 0.012 255);
  --muted-foreground: oklch(0.69 0.013 255);
  --accent: oklch(0.27 0.035 272);
  --accent-foreground: oklch(0.91 0.035 272);
  --accent-hover: oklch(0.31 0.05 272);

  /* Feedback */
  --destructive: oklch(0.68 0.19 25);
  --destructive-foreground: oklch(0.16 0.03 25);
  --destructive-subtle: oklch(0.27 0.06 25);
  --success: oklch(0.7 0.15 150);
  --success-foreground: oklch(0.16 0.03 150);
  --success-subtle: oklch(0.26 0.055 150);
  --warning: oklch(0.78 0.145 82);
  --warning-foreground: oklch(0.21 0.045 82);
  --warning-subtle: oklch(0.29 0.055 82);
  --info: oklch(0.72 0.14 245);
  --info-foreground: oklch(0.16 0.03 245);
  --info-subtle: oklch(0.27 0.055 245);

  /* Lines and focus */
  --border: oklch(0.305 0.013 255);
  --border-subtle: oklch(0.265 0.012 255);
  --border-strong: oklch(0.39 0.016 255);
  --input: oklch(0.34 0.014 255);
  --input-background: oklch(0.19 0.01 255);
  --ring: var(--brand);
  --focus-foreground: var(--foreground);

  /* Selection and code */
  --selection: color-mix(in oklch, var(--brand) 26%, transparent);
  --selection-foreground: var(--foreground);
  --code: oklch(0.15 0.009 255);
  --code-foreground: oklch(0.86 0.008 255);

  /* Sidebar */
  --sidebar: oklch(0.15 0.009 255);
  --sidebar-foreground: oklch(0.84 0.008 255);
  --sidebar-muted: oklch(0.19 0.01 255);
  --sidebar-muted-foreground: oklch(0.62 0.012 255);
  --sidebar-hover: oklch(0.215 0.012 255);
  --sidebar-active: color-mix(in oklch, var(--brand) 16%, oklch(0.17 0.01 255));
  --sidebar-active-foreground: oklch(0.9 0.04 272);
  --sidebar-primary: var(--brand);
  --sidebar-primary-foreground: var(--brand-foreground);
  --sidebar-accent: var(--sidebar-hover);
  --sidebar-accent-foreground: var(--sidebar-foreground);
  --sidebar-border: oklch(0.265 0.012 255);
  --sidebar-ring: var(--ring);
  --sidebar-section: oklch(0.58 0.012 255);
  --sidebar-icon: oklch(0.64 0.014 255);
  --sidebar-icon-active: var(--brand);

  /* Charts: slightly brighter than light theme */
  --chart-1: oklch(0.71 0.17 272);
  --chart-2: oklch(0.72 0.13 185);
  --chart-3: oklch(0.74 0.14 145);
  --chart-4: oklch(0.79 0.14 82);
  --chart-5: oklch(0.72 0.16 25);
  --chart-6: oklch(0.73 0.15 320);
  --chart-7: oklch(0.72 0.12 225);
  --chart-8: oklch(0.66 0.03 255);
  --chart-grid: oklch(0.29 0.012 255);
  --chart-axis: oklch(0.61 0.012 255);
  --chart-tooltip: var(--popover);
  --chart-tooltip-foreground: var(--popover-foreground);

  --shadow-xs: 0 1px 2px rgb(0 0 0 / 0.18);
  --shadow-sm: 0 2px 5px rgb(0 0 0 / 0.22);
  --shadow-md: 0 10px 28px rgb(0 0 0 / 0.3), 0 2px 8px rgb(0 0 0 / 0.2);
  --shadow-lg: 0 18px 48px rgb(0 0 0 / 0.4), 0 5px 16px rgb(0 0 0 / 0.24);
  --shadow-dialog: 0 28px 80px rgb(0 0 0 / 0.5), 0 8px 28px rgb(0 0 0 / 0.3);
}
```

### 4.4 Accent Color 解耦

Theme 负责明暗关系，Accent 负责品牌强调色。`success`、`warning`、`destructive`、`info` 不随 Accent 改变。

默认 Accent 为 Indigo。建议支持 Blue、Indigo、Violet、Green、Orange、Rose。每组需分别给 Light 和 Dark 校准值，不能简单复用同一个色值。

```css
/* Default: Indigo */
:root,
[data-accent="indigo"] {
  --brand: oklch(0.56 0.21 272);
  --brand-hover: oklch(0.51 0.22 272);
  --brand-active: oklch(0.47 0.20 272);
  --brand-subtle: oklch(0.95 0.03 272);
  --brand-muted: oklch(0.9 0.06 272);
  --brand-foreground: oklch(0.99 0.005 272);
}

[data-theme="dark"][data-accent="indigo"],
.dark[data-accent="indigo"] {
  --brand: oklch(0.7 0.17 272);
  --brand-hover: oklch(0.75 0.16 272);
  --brand-active: oklch(0.64 0.18 272);
  --brand-subtle: oklch(0.27 0.055 272);
  --brand-muted: oklch(0.34 0.075 272);
  --brand-foreground: oklch(0.17 0.025 272);
}

[data-accent="blue"] {
  --brand: oklch(0.56 0.19 252);
  --brand-hover: oklch(0.51 0.20 252);
  --brand-active: oklch(0.47 0.19 252);
  --brand-subtle: oklch(0.95 0.03 252);
  --brand-muted: oklch(0.9 0.06 252);
  --brand-foreground: oklch(0.99 0.005 252);
}

[data-theme="dark"][data-accent="blue"],
.dark[data-accent="blue"] {
  --brand: oklch(0.71 0.15 252);
  --brand-hover: oklch(0.76 0.14 252);
  --brand-active: oklch(0.65 0.16 252);
  --brand-subtle: oklch(0.27 0.05 252);
  --brand-muted: oklch(0.34 0.07 252);
  --brand-foreground: oklch(0.16 0.025 252);
}

[data-accent="violet"] {
  --brand: oklch(0.57 0.22 294);
  --brand-hover: oklch(0.52 0.23 294);
  --brand-active: oklch(0.48 0.21 294);
  --brand-subtle: oklch(0.95 0.035 294);
  --brand-muted: oklch(0.9 0.065 294);
  --brand-foreground: oklch(0.99 0.005 294);
}

[data-theme="dark"][data-accent="violet"],
.dark[data-accent="violet"] {
  --brand: oklch(0.72 0.17 294);
  --brand-hover: oklch(0.77 0.16 294);
  --brand-active: oklch(0.66 0.18 294);
  --brand-subtle: oklch(0.28 0.06 294);
  --brand-muted: oklch(0.35 0.08 294);
  --brand-foreground: oklch(0.17 0.025 294);
}

[data-accent="green"] {
  --brand: oklch(0.52 0.145 155);
  --brand-hover: oklch(0.47 0.15 155);
  --brand-active: oklch(0.43 0.14 155);
  --brand-subtle: oklch(0.95 0.035 155);
  --brand-muted: oklch(0.89 0.065 155);
  --brand-foreground: oklch(0.99 0.005 155);
}

[data-theme="dark"][data-accent="green"],
.dark[data-accent="green"] {
  --brand: oklch(0.7 0.135 155);
  --brand-hover: oklch(0.75 0.125 155);
  --brand-active: oklch(0.64 0.145 155);
  --brand-subtle: oklch(0.27 0.05 155);
  --brand-muted: oklch(0.34 0.07 155);
  --brand-foreground: oklch(0.16 0.025 155);
}

[data-accent="orange"] {
  --brand: oklch(0.64 0.16 55);
  --brand-hover: oklch(0.59 0.17 55);
  --brand-active: oklch(0.54 0.16 55);
  --brand-subtle: oklch(0.96 0.04 60);
  --brand-muted: oklch(0.91 0.075 60);
  --brand-foreground: oklch(0.2 0.04 55);
}

[data-theme="dark"][data-accent="orange"],
.dark[data-accent="orange"] {
  --brand: oklch(0.76 0.14 60);
  --brand-hover: oklch(0.8 0.13 60);
  --brand-active: oklch(0.7 0.15 60);
  --brand-subtle: oklch(0.29 0.055 60);
  --brand-muted: oklch(0.36 0.075 60);
  --brand-foreground: oklch(0.2 0.04 55);
}

[data-accent="rose"] {
  --brand: oklch(0.58 0.205 10);
  --brand-hover: oklch(0.53 0.215 10);
  --brand-active: oklch(0.49 0.20 10);
  --brand-subtle: oklch(0.96 0.035 10);
  --brand-muted: oklch(0.91 0.065 10);
  --brand-foreground: oklch(0.99 0.005 10);
}

[data-theme="dark"][data-accent="rose"],
.dark[data-accent="rose"] {
  --brand: oklch(0.72 0.17 10);
  --brand-hover: oklch(0.77 0.16 10);
  --brand-active: oklch(0.66 0.18 10);
  --brand-subtle: oklch(0.28 0.055 10);
  --brand-muted: oklch(0.35 0.075 10);
  --brand-foreground: oklch(0.17 0.025 10);
}
```

Accent 选择器改变 `--brand-*` 后，以下别名必须保持统一：

```css
:root {
  --primary: var(--brand);
  --primary-foreground: var(--brand-foreground);
  --primary-hover: var(--brand-hover);
  --primary-active: var(--brand-active);
  --primary-subtle: var(--brand-subtle);
  --ring: var(--brand);
  --sidebar-primary: var(--brand);
  --sidebar-primary-foreground: var(--brand-foreground);
  --sidebar-icon-active: var(--brand);
}
```

> 注意：shadcn/ui 的 `accent` 通常表示菜单悬停或弱选中表面，不等于用户选择的品牌强调色。因此用户可选强调色命名为 `brand`，再映射到 `primary`；不要直接用 Accent Picker 改写语义反馈色。

### 4.5 Density 变量

```css
/* Default */
[data-density="comfortable"] {
  --control-height-sm: 2rem;
  --control-height-md: 2.25rem;
  --control-height-lg: 2.5rem;
  --row-height: 2.75rem;
  --table-cell-padding-x: 0.75rem;
  --table-cell-padding-y: 0.625rem;
  --card-padding: 1.25rem;
  --page-padding-x: 2rem;
  --section-gap: 2rem;
}

[data-density="compact"] {
  --control-height-sm: 1.75rem;
  --control-height-md: 2rem;
  --control-height-lg: 2.25rem;
  --row-height: 2.25rem;
  --table-cell-padding-x: 0.625rem;
  --table-cell-padding-y: 0.375rem;
  --card-padding: 1rem;
  --page-padding-x: 1.5rem;
  --section-gap: 1.5rem;
}

[data-density="spacious"] {
  --control-height-sm: 2.25rem;
  --control-height-md: 2.5rem;
  --control-height-lg: 2.75rem;
  --row-height: 3.25rem;
  --table-cell-padding-x: 1rem;
  --table-cell-padding-y: 0.875rem;
  --card-padding: 1.5rem;
  --page-padding-x: 2.5rem;
  --section-gap: 2.5rem;
}
```

Density 只能改变尺寸和间距，不得改变颜色、信息架构、功能数量和字号层级。窄窗口下 `--page-padding-x` 应通过媒体查询降为 16px。

---

## 5. Tailwind CSS v4 映射

`@theme inline` 将 CSS Variables 暴露为 Tailwind 语义工具类。变量值已经是完整颜色，无需再包裹 `hsl()`。

```css
@import "tailwindcss";

@theme inline {
  /* Color */
  --color-background: var(--background);
  --color-foreground: var(--foreground);
  --color-surface: var(--surface);
  --color-surface-foreground: var(--surface-foreground);
  --color-surface-subtle: var(--surface-subtle);
  --color-surface-raised: var(--surface-raised);
  --color-surface-sunken: var(--surface-sunken);
  --color-card: var(--card);
  --color-card-foreground: var(--card-foreground);
  --color-card-hover: var(--card-hover);
  --color-popover: var(--popover);
  --color-popover-foreground: var(--popover-foreground);
  --color-dialog: var(--dialog);
  --color-dialog-foreground: var(--dialog-foreground);
  --color-primary: var(--primary);
  --color-primary-foreground: var(--primary-foreground);
  --color-primary-hover: var(--primary-hover);
  --color-primary-active: var(--primary-active);
  --color-primary-subtle: var(--primary-subtle);
  --color-secondary: var(--secondary);
  --color-secondary-foreground: var(--secondary-foreground);
  --color-secondary-hover: var(--secondary-hover);
  --color-muted: var(--muted);
  --color-muted-foreground: var(--muted-foreground);
  --color-accent: var(--accent);
  --color-accent-foreground: var(--accent-foreground);
  --color-accent-hover: var(--accent-hover);
  --color-destructive: var(--destructive);
  --color-destructive-foreground: var(--destructive-foreground);
  --color-destructive-subtle: var(--destructive-subtle);
  --color-success: var(--success);
  --color-success-foreground: var(--success-foreground);
  --color-success-subtle: var(--success-subtle);
  --color-warning: var(--warning);
  --color-warning-foreground: var(--warning-foreground);
  --color-warning-subtle: var(--warning-subtle);
  --color-info: var(--info);
  --color-info-foreground: var(--info-foreground);
  --color-info-subtle: var(--info-subtle);
  --color-border: var(--border);
  --color-border-subtle: var(--border-subtle);
  --color-border-strong: var(--border-strong);
  --color-input: var(--input);
  --color-input-background: var(--input-background);
  --color-ring: var(--ring);
  --color-code: var(--code);
  --color-code-foreground: var(--code-foreground);

  /* Sidebar */
  --color-sidebar: var(--sidebar);
  --color-sidebar-foreground: var(--sidebar-foreground);
  --color-sidebar-primary: var(--sidebar-primary);
  --color-sidebar-primary-foreground: var(--sidebar-primary-foreground);
  --color-sidebar-accent: var(--sidebar-accent);
  --color-sidebar-accent-foreground: var(--sidebar-accent-foreground);
  --color-sidebar-muted: var(--sidebar-muted);
  --color-sidebar-muted-foreground: var(--sidebar-muted-foreground);
  --color-sidebar-hover: var(--sidebar-hover);
  --color-sidebar-active: var(--sidebar-active);
  --color-sidebar-active-foreground: var(--sidebar-active-foreground);
  --color-sidebar-border: var(--sidebar-border);
  --color-sidebar-ring: var(--sidebar-ring);
  --color-sidebar-section: var(--sidebar-section);
  --color-sidebar-icon: var(--sidebar-icon);
  --color-sidebar-icon-active: var(--sidebar-icon-active);

  /* Charts */
  --color-chart-1: var(--chart-1);
  --color-chart-2: var(--chart-2);
  --color-chart-3: var(--chart-3);
  --color-chart-4: var(--chart-4);
  --color-chart-5: var(--chart-5);
  --color-chart-6: var(--chart-6);
  --color-chart-7: var(--chart-7);
  --color-chart-8: var(--chart-8);
  --color-chart-grid: var(--chart-grid);
  --color-chart-axis: var(--chart-axis);

  /* Typography */
  --font-sans: var(--font-sans);
  --font-mono: var(--font-mono);
  --text-2xs: var(--font-size-2xs);
  --text-2xs--line-height: var(--line-height-normal);
  --text-xs: var(--font-size-xs);
  --text-xs--line-height: var(--line-height-normal);
  --text-sm: var(--font-size-sm);
  --text-sm--line-height: var(--line-height-normal);
  --text-base: var(--font-size-md);
  --text-base--line-height: var(--line-height-normal);
  --text-lg: var(--font-size-lg);
  --text-lg--line-height: var(--line-height-snug);
  --text-xl: var(--font-size-xl);
  --text-xl--line-height: var(--line-height-snug);
  --text-2xl: var(--font-size-2xl);
  --text-2xl--line-height: var(--line-height-tight);
  --text-3xl: var(--font-size-3xl);
  --text-3xl--line-height: var(--line-height-tight);
  --font-weight-normal: var(--font-weight-regular);
  --font-weight-medium: var(--font-weight-medium);
  --font-weight-semibold: var(--font-weight-semibold);
  --font-weight-bold: var(--font-weight-bold);

  /* Radius and shadow */
  --radius-xs: var(--radius-xs);
  --radius-sm: var(--radius-sm);
  --radius-md: var(--radius-md);
  --radius-lg: var(--radius-lg);
  --radius-xl: var(--radius-xl);
  --shadow-xs: var(--shadow-xs);
  --shadow-sm: var(--shadow-sm);
  --shadow-md: var(--shadow-md);
  --shadow-lg: var(--shadow-lg);

  /* Breakpoints and containers */
  --breakpoint-xs: var(--breakpoint-xs);
  --breakpoint-sm: var(--breakpoint-sm);
  --breakpoint-md: var(--breakpoint-md);
  --breakpoint-lg: var(--breakpoint-lg);
  --breakpoint-xl: var(--breakpoint-xl);
  --breakpoint-2xl: var(--breakpoint-2xl);
  --breakpoint-3xl: var(--breakpoint-3xl);
  --container-reading: var(--container-reading);
  --container-chat: var(--container-chat);
  --container-workspace: var(--container-workspace);

  /* Motion */
  --ease-standard: var(--ease-standard);
  --ease-enter: var(--ease-enter);
  --ease-exit: var(--ease-exit);
  --ease-spring-soft: var(--ease-spring-soft);
}
```

映射后的业务组件应使用 `bg-background`、`text-foreground`、`bg-card`、`border-border`、`bg-primary`、`text-primary-foreground`、`bg-success-subtle`、`text-success` 等语义类。

以下方式不允许在业务组件中长期存在：`bg-white`、`dark:bg-zinc-950`、`text-black`、`border-gray-200`、`text-indigo-600`。设计系统内部的色阶预览页除外。

### 5.1 `data-theme`、`.dark` 与首屏策略

根节点示例：

```html
<html data-theme="dark" data-accent="indigo" data-density="comfortable" class="dark">
```

规则：

- `data-theme` 是主题的事实来源，支持未来的 `nord`、`warm`、`forest`、`high-contrast`。
- `.dark` 只作为 Tailwind `dark:*` 和第三方组件兼容钩子。
- 当 `data-theme="dark"` 时同步 `.dark`；其他暗色派生主题也应同步 `.dark`。
- 用户设置为 `system` 时，运行时根据 `prefers-color-scheme` 解析为实际 `data-theme`。
- 主题、Accent、Density 应持久化，并在 React 挂载前写入 `<html>`，避免首屏闪烁。
- 切换主题时默认不做全页颜色补间；只允许 120ms 以内的轻微过渡，且首次加载禁用过渡。

### 5.2 shadcn/ui 兼容说明

- 保留 shadcn 的标准变量：`background`、`foreground`、`card`、`card-foreground`、`popover`、`popover-foreground`、`primary`、`primary-foreground`、`secondary`、`secondary-foreground`、`muted`、`muted-foreground`、`accent`、`accent-foreground`、`destructive`、`border`、`input`、`ring`、`sidebar-*`、`chart-*`、`radius`。
- 新增 `success`、`warning`、`info`、`surface-*`、状态变体和布局变量，不删除标准变量。
- shadcn 组件只是无样式业务基础，视觉值必须以本文为准；复制组件后不要保留不符合规范的硬编码类。
- `destructive-foreground` 即使组件模板未使用也必须定义。
- Ring 使用 `--ring`；表单错误同时使用 `--destructive` 边框、错误文案和 `aria-invalid`，不能只改变颜色。
- Border Radius 的 shadcn 派生关系可以用 `calc(var(--radius) - 2px)`，但最终不得小于 4px。

---

## 6. 排版规范

### 6.1 字体角色

| 角色 | 字号 / 行高 | 字重 | 用途 |
|---|---|---|---|
| Page Title | 20–24 / 1.25 | 600 | 页面标题，仅一处 |
| Section Title | 16–18 / 1.375 | 600 | 区块标题 |
| Card Title | 14–16 / 1.375 | 600 | 卡片或面板标题 |
| Body | 14 / 1.5 | 400 | 默认正文、表单、列表 |
| Secondary | 13 / 1.5 | 400 | 辅助信息、元数据 |
| Caption | 12 / 1.5 | 400–500 | 标签、时间、统计 |
| Micro | 11 / 1.25 | 500 | 极小计数，仅在空间受限处 |
| Code | 13 / 1.625 | 400 | 代码块、日志、命令 |

要求：

- 不在后台工作页使用 32px 以上标题；登录页和空状态品牌区例外。
- 英文缩写和数字可用 `font-variant-numeric: tabular-nums` 保证列对齐。
- 中文段落最大阅读宽度 52rem，AI 长回答最大 62rem。
- 次要文字不能仅靠更小字号表达，必须同时保持足够对比度。
- 链接默认使用 Primary 色或下划线；正文链接 Hover 必须有下划线。

---

## 7. App Shell 与页面布局

### 7.1 桌面结构

```text
┌──────────── Sidebar ────────────┬──────────────── Main Workspace ────────────────┐
│ Brand                    Toggle │ Global/Page Header                             │
│ Search / Command                ├────────────────────────────────────────────────┤
│ Primary Navigation              │                                                │
│ Workspace / Projects            │ Page Content                 Optional AI Panel │
│ AI                              │                                                │
│ Integrations                    │                                                │
│                                 │                                                │
│ Settings / User                 │                                                │
└─────────────────────────────────┴────────────────────────────────────────────────┘
```

### 7.2 固定尺寸

- Sidebar 展开：240px；可选用户偏好范围 224–280px。
- Sidebar 折叠：64px；不可再缩小，避免 44px 点击区无法成立。
- Header：56px；窄屏 52px。
- AI Side Panel：默认 400px，可在 360–440px 范围内调整。
- 页面横向 Padding：默认 32px，Compact 24px，宽度小于 768px 时 16px。
- 页面纵向 Padding：24px。
- Header 与内容滚动区分离；只在确有价值时 Sticky。

### 7.3 内容宽度按页面类型选择

| 页面类型 | 最大内容宽度 | 说明 |
|---|---:|---|
| Dashboard | 1280–1440px | 保持数据概览易扫描 |
| Project / Task / File | 1600px 或流式 | 表格、看板需要横向空间 |
| Settings / Form | 720–960px | 防止表单行过长 |
| Note Editor | 760–832px | 阅读优先 |
| AI Chat | 832–992px | 长回答与代码兼顾 |
| Command Palette | 640px | 居中浮层 |

禁止所有页面共用一个固定 `max-width`。

### 7.4 Page Header

- 第一行：标题、可选面包屑、主操作。
- 第二行：短说明或 Tab / Filter；没有内容时不保留空行。
- 标题高度紧凑，不设计 Hero。
- 一个页面最多一个 Primary Button；其他操作用 Secondary、Outline 或 Ghost。
- 列表页 Header 下方可放 Filter Bar，滚动后允许 Sticky。

### 7.5 响应式

- `>= 1280px`：完整 Sidebar；AI Panel 可并排。
- `1024–1279px`：Sidebar 可保持展开或按用户偏好折叠；AI Panel 以覆盖层打开。
- `768–1023px`：Sidebar 默认折叠；多级子菜单以浮层呈现。
- `< 768px`：Sidebar 使用抽屉；Header 显示菜单按钮；表格允许横向滚动或切换为信息行，不强行塞入。
- 当前产品 Desktop First，但任何页面不得在 768px 以上出现内容遮挡、不可滚动或操作丢失。

---

## 8. Sidebar 与多级菜单规范

### 8.1 信息架构

Sidebar 自上而下：

1. 品牌区：Logo、产品名、折叠按钮。
2. 全局搜索 / Command Palette 入口，展示 `⌘K` 或 `Ctrl K`。
3. 一级核心导航：Dashboard、Projects、Tasks、Notes、Files、Search。
4. AI 区：AI Chat、Knowledge、Agents，可折叠为一个分组。
5. Integrations 区：外部集成、飞书等。
6. 底部固定区：Settings、Help、User Menu。

**不出现 Upgrade Pro、更新 Pro、套餐推销卡、剩余额度广告或任何占据导航空间的商业推广。** 若未来确有计费功能，只能进入 Settings > Billing，不得常驻 Sidebar。

### 8.2 展开态

- 宽度 240px，导航项高度 36px，左右内边距 10px。
- 图标 18px，文字 14px，图标与文字间距 10px。
- Active 使用 `--sidebar-active` 背景、`--sidebar-active-foreground` 文字和 `--sidebar-icon-active` 图标。
- Active 不使用整条高饱和 Primary 背景，不使用左侧粗色条与背景同时叠加。
- Hover 使用 `--sidebar-hover`；按下时允许轻微降低亮度。
- 分组标题 12px / 500，使用 `--sidebar-section`，上方间距 16px、下方 6px。
- 每项只有一个主要点击目标；行内更多操作仅在 Hover 或 Focus Within 时出现。

### 8.3 折叠入口与状态

- 折叠按钮位于品牌区右侧，图标建议 `PanelLeftClose`；折叠后使用 `PanelLeftOpen`。
- 按钮点击区至少 32×32px，Tooltip 文案为“收起侧栏”或“展开侧栏”，显示快捷键时一并呈现。
- 支持快捷键 `Ctrl/Cmd + B`；输入框聚焦时不得误触。
- 用户主动选择的展开/折叠状态需要持久化。
- 窗口自动折叠与用户偏好分开保存；恢复宽窗口时回到用户之前的状态。
- 展开与折叠动画使用 `--duration-slow` + `--ease-standard`；内容文字淡出应先于宽度收缩，避免挤压跳动。
- 页面主要内容随 Sidebar 平滑调整，不突然重排。

### 8.4 折叠态

- 宽度 64px，品牌区显示 Logo，所有导航项水平居中。
- 隐藏文字、分组标题、尾部快捷键和行内操作，但保留可访问名称。
- 每个图标必须有 Tooltip；Tooltip 延迟 300–500ms，键盘聚焦时立即显示。
- Active 仍以图标色和 36×36px 弱背景表达。
- 有子菜单的一级项在点击后打开右侧 Flyout，不直接在 64px 宽度内缩进展开。
- Flyout 宽 220–280px，显示父级标题、完整子菜单和当前选中状态；可用键盘方向键导航。
- 折叠态不可只留下无意义的小圆点或首字母。

### 8.5 多级菜单

最多支持三级。超过三级应调整信息架构，改用页面内导航、树视图或 Command Palette。

| 层级 | 图标 | 字号 | 缩进 | 高度 | 说明 |
|---|---:|---:|---:|---:|---|
| 一级 | 18px，必需 | 14px | 0 | 36px | 产品主模块 |
| 二级 | 16px，必需 | 13px | 16px | 34px | 子模块或固定视图 |
| 三级 | 14–16px，建议 | 13px | 32px | 32px | 仅复杂树形信息架构 |

二级菜单图标规范：

- 二级菜单必须显示图标，不能只用缩进和文字表达。
- 图标取自同一套 Lucide，默认 16px、`stroke-width: 1.75`。
- 图标表达页面类型或动作，如 `ListTodo`、`CalendarDays`、`Archive`，不得用不同颜色区分每一项。
- 同一父级下禁止复用同一图标；除非它们是同类动态实体，此时使用实体图标加文本区分。
- 动态项目可用 6px 颜色圆点作为附加标记，但不能取代图标。

展开控制：

- 有子项的父级行末显示 `ChevronRight`；展开旋转为向下，时长 180ms。
- 点击文字区域进入父级默认页；点击 Chevron 只控制展开。若父级不可导航，整行负责展开，交互模型必须一致。
- 展开状态按分组记忆；首次进入时自动展开包含当前路由的祖先节点。
- Active 子项必须让祖先保持展开，并让父级图标或文字进入弱激活状态。
- 展开动画只改变高度与透明度，不横向滑动整个菜单。

### 8.6 高级感的来源

“高级”不等于装饰更多，Sidebar 的精致感来自：

- 清晰分组和稳定对齐。
- 轻微但可辨识的背景层级。
- 完整图标体系和折叠态 Flyout。
- Hover 时出现的上下文操作。
- 当前路由、展开状态、通知计数与键盘焦点的一致反馈。
- 搜索、最近访问、收藏项目等提高效率的能力。

禁止给每个菜单项加描边、渐变、阴影或不同底色。

---

## 9. 图标规范

- 全局只使用 Lucide Icons；品牌 Logo 和文件类型图标可使用独立资产。
- 默认 `stroke-width: 1.75`，小于 16px 时可调整为 2。
- 常规尺寸：导航 18px、子导航 16px、按钮 16px、空状态 32–40px。
- 图标按钮必须有 `aria-label` 和 Tooltip。
- 图标与文字表达同一含义时不重复提供屏幕阅读器文本。
- Loading 不用旋转任意业务图标，统一使用 `LoaderCircle`。
- 破坏性操作图标只在确认语境下使用 Destructive 色，菜单中默认仍保持前景色。
- Emoji 不作为功能图标；用户自定义项目图标例外。

---

## 10. 组件规范

### 10.1 Button

变体：Primary、Secondary、Outline、Ghost、Destructive、Link。

| 尺寸 | 高度 | 左右 Padding | 字号 | 图标 |
|---|---:|---:|---:|---:|
| sm | 32px | 10px | 13px | 14–16px |
| md | 36px | 12px | 14px | 16px |
| lg | 40px | 16px | 14px | 18px |
| icon-sm | 32px | 0 | — | 16px |
| icon-md | 36px | 0 | — | 18px |

- 默认使用 md，圆角 `--radius-md`。
- 每个页面或 Dialog 最多一个主操作使用 Primary。
- Loading 保持原宽度，隐藏或弱化原图标并显示 Spinner，防止布局跳动。
- Disabled 使用 `--opacity-disabled`，不响应 Hover，仍保持可读。
- 危险操作首次入口可用 Ghost/Outline Destructive；最终确认按钮使用 Destructive。
- 仅图标按钮必须有 Tooltip。

### 10.2 Input / Textarea / Select / Combobox

- 默认高度 36px，背景 `--input-background`，边框 `--input`。
- Focus 使用 1px `--ring` 边框与 `--shadow-focus`，不得引发布局位移。
- Placeholder 使用前景色和 `--opacity-placeholder`。
- Error 同时显示 Destructive 边框、错误图标或文字以及可访问描述。
- Label 为 13px / 500；Description 与 Error 为 12–13px。
- Textarea 默认最小 96px，可垂直调整；编辑器类组件不套普通 Form 外观。
- Select/Combobox 菜单项高度 34–36px，支持键盘上下选择和搜索。

### 10.3 Checkbox / Radio / Switch

- Checkbox 16×16px；Radio 16×16px；Switch 32×18px 或 36×20px。
- 整个 Label 行可点击，点击区高度不低于 32px。
- Indeterminate 必须有独立视觉。
- Switch 用于立即生效的二元设置；需要保存或不可撤销的选择不用 Switch。

### 10.4 Card

- Card 用于真正的内容分组，不是所有区块的默认容器。
- 默认 `bg-card border border-border rounded-lg`，普通 Card 不加阴影。
- Hoverable Card 可改变 `--card-hover` 和边框，不上浮超过 1px。
- Card Padding 默认 20px；Compact 为 16px。
- Card 内标题、说明、操作对齐到同一网格。
- 不嵌套超过两层 Card；优先使用 Section、Divider、List。

### 10.5 Dialog / Alert Dialog / Drawer

- Dialog 宽度按内容选择：sm 400px、md 520px、lg 720px、xl 960px。
- 最大高度 `min(85vh, 900px)`，内容区独立滚动，标题与操作固定。
- 背景 `--dialog`，阴影 `--shadow-dialog`，圆角 12–16px。
- 首次打开聚焦第一个安全可操作项；破坏性 Dialog 不自动聚焦删除按钮。
- Escape 关闭普通 Dialog；有未保存修改时先拦截确认。
- Drawer 用于移动端筛选、Sidebar 和详情；桌面关键表单优先 Dialog 或独立页面。

### 10.6 Dropdown / Popover / Tooltip

- Dropdown 最小宽 180px，项高 34px，分隔线只用于语义分组。
- 菜单文字左对齐，快捷键右对齐，危险项置于底部并与普通项分隔。
- Popover 只承载轻量交互，不放复杂长表单。
- Tooltip 只补充解释，不能承载完成任务所必需的信息。
- 浮层统一使用 `--popover`、`--popover-foreground`、`--border` 和 `--shadow-md`。

### 10.7 Tabs / Segmented Control

- Tabs 用于同层内容导航；Selected 通过文字、底线或弱背景表达。
- 标签过多时允许横向滚动或进入 More，不压缩到不可读。
- Segmented Control 只用于 2–4 个互斥视图，如 List / Board。
- 不把主导航、过滤器和操作按钮混进同一 Tabs 组件。

### 10.8 Badge / Status

| 状态 | Token | 视觉 |
|---|---|---|
| Neutral / Todo | muted | 中性弱背景 |
| In Progress | info | 蓝色弱背景 |
| Done / Success | success | 绿色弱背景 |
| Warning / Blocked | warning | 琥珀弱背景 |
| Error / Urgent | destructive | 红色弱背景 |
| Cancelled / Archived | muted | 降低对比度 |

- Badge 高度 20–24px，字号 11–12px，圆角默认 6px，不默认使用全胶囊。
- 状态不能只靠颜色，必须有文字或图标。
- 不为每个 Project、Tag 分配高饱和实色背景。

### 10.9 Table / Data Grid

- Header 12–13px / 500，默认不使用深色底。
- 行高由 Density 控制；Cell 横向 Padding 10–16px。
- 只保留横向分隔或 Row Border，不给每个 Cell 画边框。
- Row Hover 使用 `--surface-subtle`；Selected 使用 `--primary-subtle`。
- 支持 Sorting、Filtering、Pagination、Selection、Column Visibility、Loading、Empty、Row Actions。
- 数字右对齐，文本左对齐，状态与操作保持稳定列宽。
- 行操作默认 Hover/Focus 时出现；高频主操作可常驻。
- 表头 Sticky 时必须有背景和下边框，避免内容透出。

### 10.10 Form

- Field 垂直结构：Label → Control → Description / Error。
- Field 间距 16px；Section 间距 24–32px。
- 单列表单优先，相关短字段可两列；窄屏自动单列。
- 必填使用文字或一致星号，不能只依赖 Placeholder。
- 提交失败后聚焦或滚动到第一个错误字段。
- 自动保存只显示安静的“正在保存 / 已保存”状态，不反复弹成功 Toast。

### 10.11 Toast / Alert / Inline Feedback

- Toast 用于跨区域的短反馈；成功自动关闭 3–4 秒，错误可保持更久。
- 表单错误优先 Inline，不用 Toast 代替字段错误。
- Alert 用于页面内持续状态；颜色使用对应 `*-subtle` 背景。
- 成功操作若结果已在界面中明显出现，不额外弹 Toast。
- Toast 同屏最多 3 条，新消息进入队列。

### 10.12 Empty State

- 构成：32–40px 简单图标、14–16px 标题、13–14px 描述、最多两个操作。
- 默认最大宽 420px，垂直居中但不制造巨大空白。
- 优先说明下一步，如“创建第一个项目”；不使用巨大插画。
- 搜索无结果与首次空数据必须使用不同文案和操作。

### 10.13 Loading / Skeleton / Progress

- 首次内容加载用 Skeleton；局部提交用按钮 Loading；未知短等待用 Inline Spinner。
- Skeleton 应模拟真实内容结构，避免整页相同灰条。
- 超过 2 秒可显示说明；可取消的长任务必须提供取消入口。
- AI Streaming 显示逐步输出和当前阶段，不展示虚假百分比。
- Loading 不清空旧内容；后台刷新应保留现有数据并给出弱提示。

### 10.14 Command Palette

- 快捷键 `Ctrl/Cmd + K`，宽 640px，最大高 70vh。
- 支持搜索、导航、创建 Task/Note、打开 Project、Ask AI。
- 结果按类型分组，显示图标、主标题、上下文和快捷键。
- 最近使用项优先，但不得打乱搜索相关性。
- 键盘上下选择、Enter 执行、Escape 返回或关闭。

### 10.15 AI Chat / AI Panel

- AI Chat 长回答采用文档流，不强制使用双侧巨大气泡。
- 用户消息可用轻背景块，AI 回复使用普通内容布局。
- 支持 Markdown、Code、Table、Sources、Tool Calls、Streaming、Retry、Copy。
- Source 使用可展开的轻量引用，不把完整 URL 暴露成视觉噪声。
- Tool Call 默认折叠，显示工具名、状态、耗时；错误时允许查看安全摘要。
- 输入区固定在底部，默认 44–120px 自适应；附件、模型和上下文选择为次级操作。
- AI Side Panel 开启时保留当前页面上下文，关闭后恢复主内容宽度和焦点。
- AI 生成的不可逆操作必须在执行前让用户确认。

---

## 11. 页面级规范

### 11.1 Dashboard

- 目标是呈现“现在最值得关注的内容”，不是堆满 KPI Card。
- 推荐结构：Today / Focus、进行中的 Projects、最近 Notes、Upcoming Tasks、AI 建议。
- 统计卡不超过 4 个；优先用 List、Section、Progress 和小型 Chart。
- 图表必须有标题、时间范围、单位、Tooltip 和无障碍摘要。

### 11.2 Projects

- 支持 List / Grid 切换，并记忆用户偏好。
- 项目项展示 Name、短 Description、Status、Progress、Due Date、Task Count。
- Project Detail 使用 Overview、Tasks、Notes、Files、Activity、AI 等页面内 Tabs。
- Card 不塞入完整成员、活动流和全部标签；详细信息进入详情页或 Inspector。

### 11.3 Tasks

- 提供 Today、Upcoming、All、Completed 和 Project 视图。
- 核心行结构：Checkbox、Title、Project、Priority、Due Date；次要操作 Hover 出现。
- 支持 List 和 Kanban；同一状态颜色在两种视图中保持一致。
- 完成任务应即时反馈但可撤销；不弹强成功 Toast。

### 11.4 Notes

- 使用 Notes Sidebar + Editor 或响应式双栏。
- Editor 宽 760–832px，保持低干扰，不设计成后台表单。
- 元数据、历史和 AI 工具进入侧栏或浮层，正文保持安静。
- 选中文本后的 AI 操作使用浮动工具条，不常驻大按钮。

### 11.5 Files

- 支持 List / Grid，展示 Icon、Name、Type、Size、Updated、Source。
- 文件类型图标可有受控色彩；Source 使用中性 Badge。
- 上传显示队列、进度、失败原因与重试，不阻塞整个页面。

### 11.6 Settings / Appearance

- 左侧二级导航必须有图标，如 `Palette`、`Bell`、`Plug`、`Shield`。
- Appearance 至少包含 Theme：System / Light / Dark；Accent：Blue / Indigo / Violet / Green / Orange / Rose；Density：Compact / Comfortable / Spacious。
- 主题预览必须同时展示背景、Card、文本、Primary、状态色和 Sidebar 片段，不能只显示一个色块。
- 更改外观即时预览并持久化；提供“恢复默认”。

---

## 12. 状态与交互规范

### 12.1 通用状态

每个可交互组件必须定义：Default、Hover、Active/Pressed、Focus Visible、Selected、Disabled、Loading、Error（适用时）。

- Hover 只在支持 Hover 的设备启用。
- Keyboard Focus 必须比 Hover 更明显，使用 Ring 而不是只改背景。
- Active 是按下瞬间，Selected 是持续状态，不得混淆。
- Disabled 不可点击且要保留语义；只读字段使用 Readonly，不冒充 Disabled。
- 异步操作防止重复提交，并保留进度或结果状态。

### 12.2 Motion

| 场景 | 时长 | Easing |
|---|---:|---|
| Hover / Press | 120ms | standard |
| Tooltip / Dropdown | 120–180ms | enter / exit |
| Accordion / Submenu | 180ms | standard |
| Sidebar / Drawer | 240ms | standard |
| Dialog | 180–240ms | enter / exit |
| 强调反馈 | 最多 320ms | spring-soft |

- 动画只解释状态变化，不做装饰性循环。
- 不使用大面积页面飞入、弹跳、旋转或复杂弹簧。
- Sidebar 文本、图标和宽度必须协调，避免折叠时跳动。
- `prefers-reduced-motion: reduce` 时关闭非必要动画，将必要状态切换降至近乎瞬时。

```css
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }
}
```

### 12.3 Drag & Drop

- 拖动开始后显示原位置占位与可落点。
- 不可落区域明确显示禁止状态。
- 键盘必须有可替代操作，如“移动到…”菜单。
- Task 看板拖动完成后提供短时 Undo。

### 12.4 保存与离开

- 自动保存：显示“正在保存 / 已保存 / 保存失败”，不频繁 Toast。
- 手动保存：按钮 Loading 后恢复；失败保持输入并提供重试。
- 有未保存内容时关闭 Dialog、切页或刷新必须提示。
- 破坏性操作必须说明对象与影响范围；能软删除时提供恢复。

---

## 13. 图表规范

- 图表色按 `--chart-1` 至 `--chart-8` 使用；单系列优先 `--chart-1`。
- 状态类数据可使用 success/warning/destructive，但不得改变其含义。
- 网格线使用 `--chart-grid`，轴与刻度使用 `--chart-axis`。
- 同图系列超过 6 个时优先改为筛选、分面或表格，而非继续堆颜色。
- 颜色之外必须提供图例、标签、形状或线型区分。
- Tooltip 使用 Popover 主题变量，数字使用 tabular nums。
- 不使用 3D 图、仪表盘式装饰、无意义面积渐变或高饱和彩虹色。

---

## 14. 可访问性

- 普通正文与背景对比度至少 4.5:1；大文本至少 3:1；控件边界与焦点至少 3:1。
- 所有功能均可键盘完成，Tab 顺序与视觉顺序一致。
- `:focus-visible` 不得被清除；Mouse Click 可以不显示强 Ring。
- Icon-only Button、Sidebar 折叠项、状态图标必须有可访问名称。
- Dialog、Popover、Menu 使用正确角色、焦点锁定与返回焦点。
- 表单错误通过 `aria-invalid`、`aria-describedby` 与可见文案关联。
- Toast 和异步状态使用适当的 Live Region，不重复朗读流式 AI 全文。
- 不仅依赖颜色、位置、Hover 或动画表达状态。
- 点击 / 触摸目标建议至少 36×36px；移动端至少 44×44px。
- 系统缩放到 200% 时核心任务仍可完成，不出现文字裁切。

---

## 15. 文案与内容规范

- 按钮使用动词：“创建项目”“保存修改”“重新生成”，避免模糊的“确定”。
- 标题使用简洁名词；描述说明用户能做什么或下一步是什么。
- 删除确认必须写明对象，如“删除项目「个人知识库」”，不要只写“确认删除？”。
- 错误信息包含问题与可执行的恢复建议，不暴露堆栈、Token 或内部服务名。
- 同一概念全局使用一个名称；不要混用 Project / 项目、Task / 待办而无规则。
- AI 输出状态使用“正在生成”“正在检索资料”“需要确认”，不拟人化夸张表达。

---

## 16. 禁止事项

### 16.1 视觉

- 禁止传统 ERP 式深色顶栏 + 饱和蓝色 Sidebar + 满屏描边表格。
- 禁止大面积纯黑 `#000`、刺眼纯白层层叠加。
- 禁止大量渐变、玻璃拟态、霓虹光、背景噪点和无意义纹理。
- 禁止普通 Card 使用重阴影；禁止每个区块都套 Card。
- 禁止过度圆角和所有控件胶囊化。
- 禁止每个状态、标签、菜单项都使用不同高饱和颜色。
- 禁止巨大 Page Title、后台 Hero 和满页营销标语。

### 16.2 导航

- 禁止 Sidebar 常驻“Upgrade Pro / 更新 Pro”或营销卡片。
- 禁止二级菜单没有图标。
- 禁止折叠后只隐藏文字却保留错位缩进。
- 禁止折叠态点击有子级菜单时没有反馈。
- 禁止同时使用左色条、实色背景、粗体和高亮图标四重 Active 效果。
- 禁止超过三级菜单；禁止同一层混用不同展开交互。

### 16.3 实现

- 禁止业务组件硬编码主题色、Dark 色和任意间距。
- 禁止直接修改 shadcn 组件而不回写设计系统规范。
- 禁止混用 Lucide、Material、Font Awesome 和 Emoji 作为功能图标。
- 禁止移除 Focus Outline 却不提供替代。
- 禁止只为鼠标设计 Hover 操作而不给键盘入口。
- 禁止用 Spinner 覆盖整个页面来处理局部更新。
- 禁止使用仅颜色可见的错误或状态。

---

## 17. 设计系统治理

### 17.1 Token 使用决策

新增视觉值前依次判断：

1. 是否已有语义 Token 可表达？
2. 是否可由现有尺寸刻度组合？
3. 是否是可复用的新语义？若是，增加 Semantic/Component Token。
4. 是否只是单页面偶发装饰？若是，优先删除或收敛，不立即污染全局 Token。

### 17.2 组件新增条件

满足以下任一条件才进入设计系统：

- 至少被两个业务模块复用。
- 交互复杂，需要统一无障碍或状态行为。
- 视觉不一致会明显影响体验，如 EmptyState、PageHeader、FilterBar。

不要提前创建几十个没有实际使用场景的抽象组件。

### 17.3 变更流程

- Token 变更必须同时检查 Light、Dark、全部 Accent 和三种 Density。
- 组件变更必须覆盖所有状态、键盘操作、长中文、英文、空值和 Loading。
- 破坏性 Token 重命名需要迁移说明，不静默删除。
- 设计稿和 Storybook/组件预览必须使用相同变量名。

---

## 18. 验收清单

### 18.1 Theme

- [ ] Light 与 Dark 均无硬编码修补类。
- [ ] System / Light / Dark 切换首屏无明显闪烁。
- [ ] Blue / Indigo / Violet / Green / Orange / Rose 与两个主题任意组合可读。
- [ ] Accent 不改变 Success / Warning / Destructive / Info。
- [ ] shadcn/ui 标准变量完整，组件无缺失色。

### 18.2 Sidebar

- [ ] 展开态宽度、折叠态宽度与动画符合 Token。
- [ ] 有明确折叠按钮、Tooltip 与 `Ctrl/Cmd + B`。
- [ ] 折叠状态持久化。
- [ ] 二级菜单全部有 16px Lucide 图标。
- [ ] 当前路由自动展开祖先菜单。
- [ ] 折叠态子菜单通过可访问 Flyout 打开。
- [ ] 键盘可以遍历、展开和执行菜单。
- [ ] Sidebar 中不存在 Upgrade Pro 或营销卡。

### 18.3 Components

- [ ] Button、Input、Select、Dialog、Dropdown、Tabs、Badge、Table、Toast、Empty、Loading 状态完整。
- [ ] Hover、Pressed、Focus、Selected、Disabled、Loading、Error 有清晰区别。
- [ ] 普通 Card 不依赖阴影分层。
- [ ] 表单错误可见且可被辅助技术读取。
- [ ] Dialog 关闭后焦点回到触发元素。

### 18.4 Layout and accessibility

- [ ] 1280、1440、1920px 下信息密度稳定。
- [ ] 1024px 下无主要操作丢失。
- [ ] 768px 下页面仍可操作，Sidebar 使用合适模式。
- [ ] 200% 缩放无关键文字裁切。
- [ ] 普通文字对比度达到 4.5:1。
- [ ] Reduced Motion 生效。
- [ ] 不依赖颜色或 Hover 传达唯一信息。

---

## 19. 推荐默认值摘要

| 项目 | 默认值 |
|---|---|
| Theme | System，解析为 Light / Dark |
| Accent | Indigo |
| Density | Comfortable |
| Body | 14px / 1.5 / 400 |
| Page Title | 20–24px / 600 |
| Control Height | 36px |
| Sidebar | 240px，折叠 64px |
| Header | 56px |
| Page Padding | 32px × 24px |
| Card Radius | 12px |
| Control Radius | 8px |
| Card Shadow | None，优先 Border |
| Floating Shadow | `--shadow-md` |
| AI Panel | 400px，可调 360–440px |
| Note Width | 760–832px |
| AI Chat Width | 832–992px |
| Transition | 120–240ms |
| Icon Library | Lucide Icons |
| Menu Depth | 最多 3 级 |

---

## 20. 最终设计原则

1. 内容优先于装饰，层级优先于颜色。
2. AI 是能力，不是视觉特效。
3. 主题与 Accent 分离，业务组件只认语义 Token。
4. Sidebar 是工作导航，不是营销位。
5. 二级菜单必须有图标；折叠后必须仍然可理解、可操作。
6. 通过排版、间距、背景和轻边框建立层级，少用阴影和高饱和色。
7. 默认高效，允许用户通过 Density 与主题调整舒适度。
8. 所有组件从一开始就包含键盘、焦点、Loading、Error 和 Reduced Motion。
9. 页面宽度按任务类型变化，不用一个模板强行覆盖所有场景。
10. 每一次新增样式都应能说明它属于哪个 Token、解决什么问题，以及在 Light/Dark 中如何表现。
