"""
Streamlit Web应用主界面 - 基于design-v1方案的可视化管理界面
"""

import streamlit as st
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
from datetime import datetime
from typing import Dict, Any
import json

from src.infrastructure.config_manager import get_config_manager
from src.core.workflow_engine import get_workflow_engine


def create_web_app():
    """创建Streamlit Web应用"""
    
    # 页面配置
    st.set_page_config(
        page_title="KN-Fetch 代码分析平台",
        page_icon="🔍",
        layout="wide",
        initial_sidebar_state="expanded"
    )
    
    # 应用标题
    st.title("🔍 KN-Fetch 代码分析平台")
    st.markdown("基于design-v1方案的代码资产扫描与语义建模系统")
    
    # 侧边栏
    with st.sidebar:
        st.header("导航")
        page = st.radio(
            "选择页面",
            ["仪表盘", "代码分析", "项目管理", "系统配置", "关于"]
        )
        
        # 系统状态
        st.header("系统状态")
        config_manager = get_config_manager()
        app_config = config_manager.get_app_config()
        
        st.metric("环境", app_config.environment.value)
        st.metric("版本", app_config.version)
        st.metric("调试模式", "启用" if app_config.debug else "禁用")
    
    # 页面路由
    if page == "仪表盘":
        show_dashboard()
    elif page == "代码分析":
        show_analysis_page()
    elif page == "项目管理":
        show_projects_page()
    elif page == "系统配置":
        show_config_page()
    elif page == "关于":
        show_about_page()


def show_dashboard():
    """显示仪表盘页面"""
    
    st.header("📊 系统仪表盘")
    
    # 统计信息卡片
    col1, col2, col3, col4 = st.columns(4)
    
    with col1:
        st.metric("分析项目数", "15")
    
    with col2:
        st.metric("总文件数", "2,847")
    
    with col3:
        st.metric("代码实体数", "18,523")
    
    with col4:
        st.metric("依赖关系数", "5,672")
    
    # 图表区域
    col1, col2 = st.columns(2)
    
    with col1:
        # 语言分布饼图
        st.subheader("编程语言分布")
        language_data = {
            "语言": ["Python", "JavaScript", "Java", "TypeScript", "其他"],
            "文件数": [1200, 850, 450, 247, 100]
        }
        
        fig = px.pie(language_data, values="文件数", names="语言")
        st.plotly_chart(fig, use_container_width=True)
    
    with col2:
        # 分析趋势图
        st.subheader("分析趋势")
        trend_data = {
            "日期": ["2024-01", "2024-02", "2024-03", "2024-04", "2024-05"],
            "分析项目数": [3, 5, 8, 12, 15]
        }
        
        fig = px.line(trend_data, x="日期", y="分析项目数", markers=True)
        st.plotly_chart(fig, use_container_width=True)
    
    # 最近分析项目
    st.subheader("最近分析项目")
    
    recent_projects = [
        {
            "项目名称": "电商平台",
            "分析时间": "2024-05-15",
            "文件数": 583,
            "状态": "已完成"
        },
        {
            "项目名称": "内容管理系统",
            "分析时间": "2024-05-10",
            "文件数": 324,
            "状态": "已完成"
        },
        {
            "项目名称": "数据分析工具",
            "分析时间": "2024-05-08",
            "文件数": 156,
            "状态": "进行中"
        }
    ]
    
    df = pd.DataFrame(recent_projects)
    st.dataframe(df, use_container_width=True)


def show_analysis_page():
    """显示代码分析页面"""
    
    st.header("🔍 代码分析")
    
    # 分析配置
    with st.form("analysis_config"):
        col1, col2 = st.columns(2)
        
        with col1:
            project_path = st.text_input(
                "项目路径",
                placeholder="/path/to/your/project",
                help="请输入要分析的代码项目路径"
            )
            
            analysis_type = st.selectbox(
                "分析类型",
                ["完整分析", "快速扫描", "架构分析", "业务分析"]
            )
        
        with col2:
            enable_llm = st.checkbox("启用LLM深度分析", value=True)
            
            output_format = st.selectbox(
                "输出格式",
                ["Markdown文档", "JSON数据", "HTML报告"]
            )
        
        submitted = st.form_submit_button("开始分析")
        
        if submitted:
            if not project_path:
                st.error("请输入项目路径")
            else:
                # 模拟分析过程
                with st.spinner("正在分析代码..."):
                    # 这里应该调用工作流引擎
                    st.success("分析任务已提交！")
                    
                    # 显示分析进度
                    progress_bar = st.progress(0)
                    
                    for percent_complete in range(100):
                        # 模拟进度更新
                        progress_bar.progress(percent_complete + 1)
                        
                    st.success("分析完成！")
                    
                    # 显示分析结果摘要
                    show_analysis_summary()


def show_analysis_summary():
    """显示分析结果摘要"""
    
    st.subheader("分析结果摘要")
    
    # 结果统计
    col1, col2, col3, col4 = st.columns(4)
    
    with col1:
        st.metric("总文件数", "583")
    
    with col2:
        st.metric("代码实体", "5,148")
    
    with col3:
        st.metric("总代码行", "18,553")
    
    with col4:
        st.metric("架构模块", "25")
    
    # 详细结果标签页
    tab1, tab2, tab3, tab4 = st.tabs(["架构分析", "业务逻辑", "代码质量", "依赖关系"])
    
    with tab1:
        st.subheader("架构分析结果")
        
        # 架构层次
        st.write("#### 架构层次")
        architecture_data = {
            "层次": ["表现层", "业务层", "数据层", "基础设施层"],
            "模块数": [8, 6, 5, 6]
        }
        
        fig = px.bar(architecture_data, x="层次", y="模块数")
        st.plotly_chart(fig, use_container_width=True)
        
        # 设计模式检测
        st.write("#### 设计模式检测")
        patterns = ["单例模式", "工厂模式", "观察者模式", "策略模式"]
        for pattern in patterns:
            st.write(f"- ✅ {pattern}")
    
    with tab2:
        st.subheader("业务逻辑分析")
        
        # 业务领域
        st.write("#### 业务领域识别")
        domains = ["用户管理", "订单交易", "产品管理", "内容管理"]
        for domain in domains:
            st.write(f"- 🏢 {domain}")
        
        # 业务流程
        st.write("#### 主要业务流程")
        processes = ["用户注册流程", "下单支付流程", "内容发布流程"]
        for process in processes:
            st.write(f"- 🔄 {process}")
    
    with tab3:
        st.subheader("代码质量评估")
        
        # 质量指标
        metrics = {
            "指标": ["代码复杂度", "重复代码", "注释覆盖率", "测试覆盖率"],
            "得分": [85, 92, 78, 65],
            "等级": ["良好", "优秀", "中等", "需改进"]
        }
        
        df = pd.DataFrame(metrics)
        st.dataframe(df, use_container_width=True)
    
    with tab4:
        st.subheader("依赖关系分析")
        
        # 依赖图可视化
        st.write("#### 模块依赖关系")
        
        # 简化的依赖关系数据
        dependencies = [
            {"source": "用户服务", "target": "认证模块", "type": "调用"},
            {"source": "订单服务", "target": "支付模块", "type": "依赖"},
            {"source": "内容服务", "target": "存储模块", "type": "使用"}
        ]
        
        for dep in dependencies:
            st.write(f"- {dep['source']} → {dep['target']} ({dep['type']})")


def show_projects_page():
    """显示项目管理页面"""
    
    st.header("📁 项目管理")
    
    # 项目列表
    projects = [
        {
            "名称": "电商平台",
            "路径": "/projects/ecommerce",
            "最后分析": "2024-05-15",
            "状态": "活跃"
        },
        {
            "名称": "内容管理系统", 
            "路径": "/projects/cms",
            "最后分析": "2024-05-10",
            "状态": "活跃"
        },
        {
            "名称": "数据分析工具",
            "路径": "/projects/analytics",
            "最后分析": "2024-05-08",
            "状态": "分析中"
        }
    ]
    
    # 项目操作
    col1, col2 = st.columns([3, 1])
    
    with col1:
        st.subheader("项目列表")
    
    with col2:
        if st.button("🔄 刷新项目"):
            st.rerun()
    
    # 项目表格
    df = pd.DataFrame(projects)
    st.dataframe(df, use_container_width=True)
    
    # 项目详情
    st.subheader("项目详情")
    
    selected_project = st.selectbox(
        "选择项目查看详情",
        [p["名称"] for p in projects]
    )
    
    if selected_project:
        project = next(p for p in projects if p["名称"] == selected_project)
        
        col1, col2 = st.columns(2)
        
        with col1:
            st.write(f"**项目名称**: {project['名称']}")
            st.write(f"**项目路径**: {project['路径']}")
            st.write(f"**最后分析**: {project['最后分析']}")
            st.write(f"**项目状态**: {project['状态']}")
        
        with col2:
            # 项目操作按钮
            if st.button("📊 查看分析报告"):
                st.info("打开分析报告页面...")
            
            if st.button("🔄 重新分析"):
                st.info("开始重新分析项目...")
            
            if st.button("🗑️ 删除项目"):
                st.warning("确认删除项目？此操作不可逆。")


def show_config_page():
    """显示系统配置页面"""
    
    st.header("⚙️ 系统配置")
    
    config_manager = get_config_manager()
    
    # 基础配置
    st.subheader("基础配置")
    
    with st.form("base_config"):
        app_config = config_manager.get_app_config()
        
        col1, col2 = st.columns(2)
        
        with col1:
            app_name = st.text_input("应用名称", value=app_config.name)
            app_version = st.text_input("版本号", value=app_config.version)
            log_level = st.selectbox(
                "日志级别",
                ["DEBUG", "INFO", "WARNING", "ERROR"],
                index=["DEBUG", "INFO", "WARNING", "ERROR"].index(app_config.log_level)
            )
        
        with col2:
            output_dir = st.text_input("输出目录", value=app_config.output_dir)
            temp_dir = st.text_input("临时目录", value=app_config.temp_dir)
            debug_mode = st.checkbox("调试模式", value=app_config.debug)
        
        if st.form_submit_button("保存基础配置"):
            st.success("基础配置已保存")
    
    # AI配置
    st.subheader("AI配置")
    
    with st.form("ai_config"):
        ai_config = config_manager.get_ai_config()
        
        provider = st.selectbox(
            "AI提供商",
            ["volcengine", "openai", "azure_openai", "anthropic"],
            index=["volcengine", "openai", "azure_openai", "anthropic"].index(ai_config.provider)
        )
        
        api_key = st.text_input(
            "API密钥",
            value=ai_config.api_key,
            type="password"
        )
        
        base_url = st.text_input("API地址", value=ai_config.base_url)
        model = st.text_input("模型名称", value=ai_config.model)
        
        enable_llm = st.checkbox("启用LLM分析", value=ai_config.enable_llm)
        timeout = st.number_input("超时时间(秒)", value=ai_config.timeout, min_value=1)
        
        if st.form_submit_button("保存AI配置"):
            st.success("AI配置已保存")
    
    # 数据库配置
    st.subheader("数据库配置")
    
    with st.form("db_config"):
        db_config = config_manager.get_database_config()
        
        tab1, tab2, tab3 = st.tabs(["PostgreSQL", "Neo4j", "Milvus"])
        
        with tab1:
            st.write("PostgreSQL配置")
            pg_host = st.text_input("主机", value=db_config.postgresql.get("host", "") if db_config.postgresql else "")
            pg_port = st.number_input("端口", value=db_config.postgresql.get("port", 5432) if db_config.postgresql else 5432)
            pg_user = st.text_input("用户名", value=db_config.postgresql.get("user", "") if db_config.postgresql else "")
            pg_password = st.text_input("密码", type="password", value=db_config.postgresql.get("password", "") if db_config.postgresql else "")
        
        with tab2:
            st.write("Neo4j配置")
            # Neo4j配置字段
        
        with tab3:
            st.write("Milvus配置")
            # Milvus配置字段
        
        if st.form_submit_button("保存数据库配置"):
            st.success("数据库配置已保存")


def show_about_page():
    """显示关于页面"""
    
    st.header("ℹ️ 关于")
    
    config_manager = get_config_manager()
    app_config = config_manager.get_app_config()
    
    st.write(f"**应用名称**: {app_config.name}")
    st.write(f"**版本**: {app_config.version}")
    st.write(f"**运行环境**: {app_config.environment.value}")
    st.write(f"**构建时间**: 2024-05-20")
    
    st.write("")
    st.write("### 系统特性")
    
    features = [
        "🔍 多语言代码分析与扫描",
        "🧠 LLM增强的语义理解",
        "🏗️ 智能架构分析与评估", 
        "💼 业务逻辑识别与建模",
        "📚 自动化文档生成",
        "📊 可视化分析与报告"
    ]
    
    for feature in features:
        st.write(f"- {feature}")
    
    st.write("")
    st.write("### 技术栈")
    
    tech_stack = [
        "**后端**: Python, FastAPI, SQLAlchemy",
        "**前端**: Streamlit, Plotly",
        "**数据库**: PostgreSQL, Neo4j, Milvus",
        "**AI**: 火山引擎, OpenAI",
        "**分析**: AST解析, 语义分析"
    ]
    
    for tech in tech_stack:
        st.write(f"- {tech}")
    
    st.write("")
    st.write("### 开源协议")
    st.write("本项目基于 MIT 协议开源")


if __name__ == "__main__":
    create_web_app()