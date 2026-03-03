#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
HiFiTi 音乐信息提取示例
演示如何从 HiFiTi 网站提取歌曲信息和音频 URL
"""

import os
import re
import sys
import requests
from urllib.parse import unquote
from bs4 import BeautifulSoup


class HiFiTiExtractor:
    """HiFiTi 音乐信息提取器"""
    
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        })
    
    def decode_url_key(self, encoded_key: str) -> str:
        """
        解码 HiFiTi 的 URL key
        
        编码规则:
        + (加号) -> _2B
        / (斜杠) -> _2F
        
        Args:
            encoded_key: 编码后的 key
            
        Returns:
            解码后的 key
        """
        decoded = encoded_key.replace('_2B', '+').replace('_2F', '/')
        return decoded
    
    def extract_from_javascript(self, html: str) -> dict:
        """
        从 JavaScript 代码中提取音乐信息
        
        在 <script> 标签中查找 APlayer 的初始化代码
        
        Args:
            html: 页面 HTML 内容
            
        Returns:
            包含歌曲信息的字典
        """
        # 正则匹配 APlayer 配置
        # 格式: name:'歌曲名',artist:'艺术家',url:'URL',cover:'封面'
        pattern = r"name:'([^']+)',artist:'([^']+)',url:'([^']+)',cover:'([^']+)'"
        matches = re.search(pattern, html)
        
        if not matches:
            return None
        
        song_name = matches.group(1)
        artist = matches.group(2)
        encoded_url = matches.group(3)
        cover_url = matches.group(4)
        
        # 解码音频 URL
        audio_url = encoded_url  # URL 本身不需要解码，key 已经在 URL 中
        
        return {
            'song_name': song_name,
            'artist': artist,
            'audio_url': audio_url,
            'cover_url': cover_url,
            'encoded_url': encoded_url
        }
    
    def extract_from_title(self, html: str) -> dict:
        """
        从页面标题提取歌曲信息
        
        标题格式: "艺术家《歌曲名》[格式] - 网站名"
        
        Args:
            html: 页面 HTML 内容
            
        Returns:
            包含歌曲信息的字典
        """
        soup = BeautifulSoup(html, 'html.parser')
        title = soup.find('title')
        
        if not title:
            return None
        
        title_text = title.get_text()
        
        # 正则匹配: 艺术家《歌曲名》[格式]
        pattern = r'([^《]+)《([^》]+)》\[([^\]]+)\]'
        matches = re.search(pattern, title_text)
        
        if not matches:
            return None
        
        return {
            'artist': matches.group(1),
            'song_name': matches.group(2),
            'format': matches.group(3)
        }
    
    def extract_lyrics(self, html: str) -> str:
        """
        提取歌词
        
        Args:
            html: 页面 HTML 内容
            
        Returns:
            歌词文本
        """
        soup = BeautifulSoup(html, 'html.parser')
        
        # 查找 <h5>歌词</h5> 后面的 <p> 标签
        h5_tags = soup.find_all('h5')
        for h5 in h5_tags:
            if '歌词' in h5.get_text():
                lyrics_p = h5.find_next_sibling('p')
                if lyrics_p:
                    # 替换 <br> 为换行符
                    lyrics = lyrics_p.get_text(separator='\n', strip=True)
                    return lyrics
        
        return None
    
    def extract_download_link(self, html: str) -> dict:
        """
        提取下载链接
        
        Args:
            html: 页面 HTML 内容
            
        Returns:
            包含下载信息的字典
        """
        soup = BeautifulSoup(html, 'html.parser')
        
        result = {
            'download_url': None,
            'extraction_code': None,
            'backup_url': None
        }
        
        # 查找 <h5>下载</h5> 后面的链接
        h5_tags = soup.find_all('h5')
        for h5 in h5_tags:
            if '下载' in h5.get_text():
                link_p = h5.find_next_sibling('p')
                if link_p:
                    link_a = link_p.find('a')
                    if link_a:
                        result['download_url'] = link_a.get('href')
        
        return result
    
    def get_audio_file_url(self, getmusic_url: str) -> str:
        """
        获取真实的音频文件 URL
        
        访问 getmusic.htm 可能会重定向到真实的音频文件
        
        Args:
            getmusic_url: getmusic.htm 的 URL
            
        Returns:
            真实的音频文件 URL
        """
        try:
            response = self.session.get(getmusic_url, allow_redirects=True)
            return response.url
        except Exception as e:
            print(f"获取音频文件 URL 失败: {e}")
            return None
    
    def sanitize_filename(self, name: str) -> str:
        """移除文件名中的非法字符"""
        illegal_chars = r'[\/\\:*?"<>|]'
        return re.sub(illegal_chars, '-', name).strip()

    def detect_extension(self, url: str) -> str:
        """从 URL 中检测音频文件扩展名，默认返回 .mp3"""
        for ext in ('.mp3', '.flac', '.m4a', '.wav', '.aac', '.ogg'):
            if ext in url.lower():
                return ext
        return '.mp3'

    def extract_all(self, url: str) -> dict:
        """
        提取页面的所有信息
        
        Args:
            url: 页面 URL
            
        Returns:
            包含所有信息的字典
        """
        try:
            response = self.session.get(url)
            response.raise_for_status()
            html = response.text
            
            js_info = self.extract_from_javascript(html)
            title_info = self.extract_from_title(html)
            lyrics = self.extract_lyrics(html)
            download_info = self.extract_download_link(html)
            
            result = {
                'url': url,
                'from_javascript': js_info,
                'from_title': title_info,
                'lyrics': lyrics,
                'download': download_info
            }
            
            if js_info and js_info.get('audio_url'):
                print("正在获取真实音频 URL...")
                real_url = self.get_audio_file_url(js_info['audio_url'])
                result['real_audio_url'] = real_url
            
            return result
            
        except Exception as e:
            print(f"提取失败: {e}")
            return None

    def download_music(self, url: str, output_dir: str = './downloads') -> str:
        """
        从 hifiti.com 页面提取并下载 MP3 文件
        
        Args:
            url: hifiti.com 音乐页面 URL
            output_dir: 下载保存目录
            
        Returns:
            下载的文件路径，失败返回 None
        """
        print(f"正在分析: {url}")
        result = self.extract_all(url)
        
        if not result:
            print("页面解析失败！")
            return None
        
        real_url = result.get('real_audio_url')
        if not real_url:
            print("未能获取到真实音频 URL！")
            return None
        
        # 构造文件名：艺术家 - 歌曲名.扩展名
        js_info = result.get('from_javascript', {})
        title_info = result.get('from_title', {})
        
        song_name = (js_info or {}).get('song_name') or (title_info or {}).get('song_name') or 'unknown'
        artist = (js_info or {}).get('artist') or (title_info or {}).get('artist') or 'unknown'
        
        song_name = self.sanitize_filename(song_name)
        artist = self.sanitize_filename(artist)
        ext = self.detect_extension(real_url)
        
        filename = f"{artist} - {song_name}{ext}"
        
        os.makedirs(output_dir, exist_ok=True)
        filepath = os.path.join(output_dir, filename)
        
        print(f"正在下载: {filename}")
        print(f"源地址: {real_url[:80]}...")
        
        try:
            resp = self.session.get(real_url, stream=True)
            resp.raise_for_status()
            
            total_size = int(resp.headers.get('content-length', 0))
            downloaded = 0
            
            with open(filepath, 'wb') as f:
                for chunk in resp.iter_content(chunk_size=8192):
                    f.write(chunk)
                    downloaded += len(chunk)
                    if total_size > 0:
                        pct = downloaded / total_size * 100
                        print(f"\r下载进度: {pct:.1f}% ({downloaded // 1024}KB / {total_size // 1024}KB)", end='', flush=True)
            
            print(f"\n下载完成: {filepath}")
            return filepath
            
        except Exception as e:
            print(f"\n下载失败: {e}")
            if os.path.exists(filepath):
                os.remove(filepath)
            return None


def main():
    """主函数 - 支持命令行传入 URL"""
    
    extractor = HiFiTiExtractor()
    
    if len(sys.argv) < 2:
        print("用法: python hifiti_extractor.py <URL> [URL2] [URL3] ...")
        print("示例: python hifiti_extractor.py https://www.hifiti.com/thread-1394.htm")
        print("\n将使用默认示例 URL 进行演示...\n")
        urls = ['https://www.hifiti.com/thread-1394.htm']
    else:
        urls = sys.argv[1:]
    
    for i, url in enumerate(urls):
        if len(urls) > 1:
            print(f"\n[{i+1}/{len(urls)}] ", end='')
        print("=" * 60)
        
        filepath = extractor.download_music(url)
        
        if filepath:
            print(f"✅ 保存到: {filepath}")
        else:
            print(f"❌ 下载失败: {url}")
        
        print("=" * 60)


if __name__ == '__main__':
    main()
