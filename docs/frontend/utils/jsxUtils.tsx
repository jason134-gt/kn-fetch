/**
 * JSX 工具函数
 */

import React from 'react';

/**
 * 高亮关键词
 */
export const highlightKeyword = (text: string, keyword: string): React.ReactNode => {
  if (!keyword) return text;
  const regex = new RegExp(`(${keyword})`, 'gi');
  const parts = text.split(regex);
  return parts.map((part, index) =>
    part.toLowerCase() === keyword.toLowerCase()
      ? <mark key={index} style={{ background: '#ffe58f', padding: '0 2px' }}>{part}</mark>
      : part
  );
};
