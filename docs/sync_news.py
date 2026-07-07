"""
长春师范大学官网新闻自动同步脚本
从 news.ccsfu.edu.cn 抓取新闻标题，解析详情后写入 AI智慧园区数据库
用法: python sync_news.py        # 抓取最新新闻
      python sync_news.py --all  # 抓取全部页
"""
import requests, re, time, sys, os, argparse
from bs4 import BeautifulSoup
from datetime import datetime

# ---------- 数据库配置 ----------
DB_CONFIG = {
    "host": os.getenv("MYSQL_HOST", "localhost"),
    "user": os.getenv("MYSQL_USER", "root"),
    "password": os.getenv("MYSQL_PASSWORD", ""),
    "database": os.getenv("MYSQL_DATABASE", "ai_smart_campus"),
}

# ---------- 类别映射 ----------
CATEGORY_KEYWORDS = {
    "notice": ["通知", "关于", "开展", "召开", "举办", "启动", "组织", "部署"],
    "activity": ["活动", "大赛", "比赛", "文化节", "运动会", "音乐会", "典礼", "表彰", "毕业"],
    "policy": ["规定", "办法", "条例", "制度", "管理", "细则"],
}

def guess_category(title):
    for cat, keywords in CATEGORY_KEYWORDS.items():
        for kw in keywords:
            if kw in title:
                return cat
    return "notice"


def fetch_page(page=1):
    """抓取新闻列表页"""
    url = f"https://news.ccsfu.edu.cn/list_search.jsp?wbtreeid=1001&a1137t=344&a1137p={page}&a1137c=15"
    headers = {"User-Agent": "Mozilla/5.0"}
    try:
        r = requests.get(url, timeout=10, headers=headers)
        r.encoding = "utf-8"
        soup = BeautifulSoup(r.text, "lxml")
        items = []
        for a in soup.select("a[href*='info']"):
            text = a.get_text(strip=True)
            href = a.get("href", "")
            if text and len(text) > 5:
                full_url = href if href.startswith("http") else f"https://news.ccsfu.edu.cn/{href.lstrip('/')}"
                items.append({"title": text, "url": full_url})
        return items
    except Exception as e:
        print(f"  [ERROR] 抓取列表页失败: {e}")
        return []

def fetch_detail(url):
    """抓取新闻详情页的正文"""
    headers = {"User-Agent": "Mozilla/5.0"}
    try:
        r = requests.get(url, timeout=10, headers=headers)
        r.encoding = "utf-8"
        soup = BeautifulSoup(r.text, "lxml")
        # 去除 script/style
        for tag in soup(["script", "style"]):
            tag.decompose()
        # 尝试找正文区域
        content_div = soup.select_one(".content, .article, .detail, .news-content, #content, .con_text")
        if content_div:
            text = content_div.get_text(strip=True)
        else:
            text = soup.get_text(strip=True)
        # 清理
        text = re.sub(r"\s+", " ", text).strip()
        return text[:2000] if text else ""
    except Exception as e:
        print(f"  [ERROR] 抓取详情失败 {url}: {e}")
        return ""


def insert_into_db(items):
    """将新闻写入数据库"""
    try:
        import pymysql
    except ImportError:
        print("[ERROR] 请先安装 pymysql: pip install pymysql")
        return 0

    conn = pymysql.connect(**DB_CONFIG, charset="utf8mb4")
    cur = conn.cursor()

    # 查询已存在的标题（避免重复）
    cur.execute("SELECT title FROM t_announcement WHERE status=1")
    existing = {row[0] for row in cur.fetchall()}

    inserted = 0
    for item in items:
        title = item["title"]
        if title in existing:
            continue

        summary = title
        content = item.get("content", "") or title
        category = guess_category(title)

        cur.execute(
            "INSERT INTO t_announcement (title, content, summary, category, author_id, created_at) VALUES (%s, %s, %s, %s, 5, NOW())",
            (title, content, summary, category),
        )
        inserted += 1
        print(f"  [+] {title[:50]}")
        print(f"      [{category}] {item['url']}")

    conn.commit()
    cur.close()
    conn.close()
    return inserted


def main():
    parser = argparse.ArgumentParser(description="同步长春师范大学官网新闻")
    parser.add_argument("--all", action="store_true", help="抓取所有页（默认只抓最新页）")
    parser.add_argument("--pages", type=int, default=3, help="抓取页数（--all 时生效）")
    args = parser.parse_args()

    max_pages = args.pages if args.all else 1

    all_items = []
    for page in range(1, max_pages + 1):
        print(f"正在抓取第 {page} 页...")
        items = fetch_page(page)
        if not items:
            print(f"  第 {page} 页无数据，停止翻页")
            break
        print(f"  找到 {len(items)} 条")
        all_items.extend(items)
        time.sleep(1)

    # 抓取详情
    print(f"\n共 {len(all_items)} 条，开始抓取详情...")
    for i, item in enumerate(all_items):
        sys.stdout.write(f"\r  [{i+1}/{len(all_items)}] {item['title'][:40]}")
        sys.stdout.flush()
        item["content"] = fetch_detail(item["url"])
        time.sleep(0.5)
    print()

    # 写入数据库
    print(f"\n写入数据库...")
    count = insert_into_db(all_items)
    print(f"完成！新增 {count} 条公告")


if __name__ == "__main__":
    main()
