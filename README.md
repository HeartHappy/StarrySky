## 使用方法

### 一、FireworksView(烟花控件)
####  FireworksView控件自定义属性
| 属性名称 |作用说明| 值/类型 | 默认值 |
| :------: | :------: | :------: | :--:|
|firTitleText|标题文本 | reference | 引用strings.xml文件中title_first字符数组 |
|firTitleSize|标题文本大小(单位/像素px) | float |72 |
|firTitleLastLineNullCharNum|标题文本最后一行空出字符数量 | integer | 2 |
|firTitleAnimDuration|标题文本动画时长(单位/秒) | integer | 5 |
|firContentText|内容文本 | reference | 引用strings.xml文件中title_content字符数组 |
|firContentSize|内容文本大小(单位/像素px) | float |48 |
|firContentLastLineNullCharNum|内容文本最后一行空出字符数量 | integer | 8 |
|firContentAnimDuration|内容标题文本动画时长(单位/秒) | integer | 10 |
|firTextVerticalSpacing|文本垂直行间距 | integer | 36 |
|firEndText|结束文本 | string | 引用strings.xml文件中title_last字符串 |
|firEndTextVerticalOffset|结束文本垂直偏移量 | integer | 0(0为居中显示) |
|firTotalDuration|烟花总时长(单位/秒) | integer | 30 |
|firTotalCount|烟花总数量 | integer | 100 |
|firTruncationCount|烟花升起时贝塞尔曲线截断数量 | float | 16 |
|firHeartCount|爱心烟花数量 | integer | 1 |
|firIsAssignAnim|配合指定开始动画使用 | boolean | false |
|firAnimatorType|指定开始动画 | enum | title |

##### 详解：firAnimatorType属性分别有：title、content、fireworks、fireworks_path、flame_heart、segmentation。当firIsAssignAnim属性为false时：firAnimatorType指定动画播放结束后，会继续播放后面的其他动画；为true时：播放完当前动画直接停止

#### 动画监听
##### 1、实现全部监听
```
fv.animatorEndListener=object :FireworksView.AnimatorEndListener{
            override fun onTitleAnimEnd() {
                //标题动画结束
            }

            override fun onContentAnimEnd() {
                //内容动画结束
            }

            override fun onFireworksAnimEnd() {
                //烟花动画结束
            }

            override fun onFireworksPathAnimEnd() {
                //爱心烟花动画结束
            }

            override fun onFlameHeartPathAnimEnd() {
                //蓝色焰心升起动画结束
            }

            override fun onSegmentationAnimEnd() {
                //窗帘拉开帷幕动画结束
            }

            override fun onOutputTextAnimEnd() {
                //结束文本动画结束
            }
        }
```
#####  2、指定实现监听
````
fv.animatorEndListener = object : FireworksView.AnimatorEndListenerAdapter() {
            override fun onContentAnimEnd() {
                super.onContentAnimEnd()
            }

            override fun onOutputTextAnimEnd() {
                super.onOutputTextAnimEnd()
                
            }
        }
````

### 二、StarrySkyView(行星控件)
#### StarrySkyView控件自定义属性
| 属性名称 |作用说明| 值/类型 | 默认值 |
| :------: | :------: | :------: | :--:|
|planetNum|行星数量(行星，移动)| integer | 5 |
|planetColor|行星颜色 | color |GRAY(灰色) |
|planetRadius|行星半径 | float | 10 |

|littleStarNum|恒星数量(恒星，不移动) | integer | 20 |
|littleStarColor|恒星颜色 | color | GRAY(灰色) |
|littleStarRadius|恒星半径 | float |5 |

|starLineColor|连接线颜色 | color | GRAY(灰色)|
|starColorChange|是否支持颜色改变 | boolean | false |
|isCancelLine|是否取消连接线 | boolean | false |

