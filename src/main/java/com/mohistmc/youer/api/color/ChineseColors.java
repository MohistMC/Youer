package com.mohistmc.youer.api.color;

import lombok.Getter;

/**
 * 中国传统颜色枚举
 * 包含中国色颜色色谱和中国传统色彩标准色卡
 */
@Getter
public enum ChineseColors {

    // 中国色颜色色谱
    BAICAOSHUANG("百草霜", "#303030"),
    BAIFANGHUILAN("柏坊灰蓝", "#4e1892"),
    BAOLAN("宝蓝", "#1f3696"),
    BEIJINGMAOLAN("北京毛蓝", "#276893"),
    BIYUSHI("碧玉石", "#569597"),
    CANGHUANG("苍黄", "#c65306"),
    ZANGLAN("藏蓝", "#25386b"),
    CANGLV("苍绿", "#4e5f45"),
    CAOHUANG("草黄", "#dbce54"),
    CHENGDEHUI("承德灰", "#757570"),
    CHENGDEZAO("承德皂", "#5a5c5b"),
    CHENSHA("辰砂", "#af5e53"),
    CHUNLAN("春蓝", "#7ba1a8"),
    CHUNLV("春绿", "#e3efd1"),
    CUILV("翠绿", "#006e5f"),
    CUJINGZAO("粗晶皂", "#43454a"),
    DACHIJIN("大赤金", "#6d7358"),
    DAILAN("黛蓝", "#304758"),
    DANDONGSHI("丹东石", "#d7c16b"),
    DANHUILV("淡灰绿", "#aec4b7"),
    DENGCAOHUI("灯草灰", "#363532"),
    DIANLAN("靛蓝", "#1b54f2"),
    FANQIEHONG("蕃茄红", "#c4473d"),
    FEIHONG("妃红", "#c35655"),
    GANCAOHUANG("甘草黄", "#e4cf8e"),
    GANLANLV("橄榄绿", "#6a6834"),
    GANSHIFEN("甘石粉", "#eadcd6"),
    GULAN("钴蓝", "#6493af"),
    GUOHUI("果灰", "#88aea3"),
    HAILAN("海蓝", "#17507d"),
    HONGZAO("红皂", "#4f5355"),
    HUANGHUI("黄灰", "#b0b7ac"),
    HUAQING("花青", "#546b83"),
    HUFEN("胡粉", "#ebe8db"),
    HUILAN("灰蓝", "#5d828a"),
    HUILV("灰绿", "#5c8987"),
    HUIMI("灰米", "#b6b196"),
    JIANGHUANG("姜黄", "#b49436"),
    JIANGXIAONI("将校呢", "#6d614a"),
    JIANGZI("绛紫", "#704d4e"),
    JUHONG("桔红", "#e7693f"),
    JUHONG2("桔黄", "#e8853b"),
    JINHUANG("金黄", "#c77a3a"),
    JUNLV("军绿", "#cad4ba"),
    KONGQUELAN("孔雀蓝", "#0041a5"),
    KUJIN("库金", "#85794f"),
    KULV("枯绿", "#b7b278"),
    LABAI("蜡白", "#e7e5d0"),
    LAOLV("老绿", "#3d6e53"),
    LIUHUAHONG("榴花红", "#d54b44"),
    LUHUI("芦灰", "#a9b08f"),
    MEIGUIHONG("玫瑰红", "#973444"),
    MEIGUIHUI("玫瑰灰", "#793d56"),
    MIHONG("米红", "#e1bda2"),
    MIHUI("米灰", "#c5bfad"),
    MISE("米色", "#f5f5dc"),
    NAILV("奶绿", "#afc8ba"),
    NAIZONG("奶棕", "#c1a299"),
    NINGMENGHUANG("柠檬黄", "#e9db39"),
    PINHONG("品红", "#a71368"),
    QIANHAICHANGLAN("浅海昌蓝", "#3c5e91"),
    QIANHUANGZONG("浅黄棕", "#dea87a"),
    QIANJUHUANG("浅桔黄", "#da9558"),
    QIANNIUZI("牵牛紫", "#a22076"),
    QIANSHIYINGZI("浅石英紫", "#ab96c5"),
    QIANTENGZI("浅藤紫", "#c4c3cb"),
    QIANTUOSE("浅驼色", "#c9ae8c"),
    QIANXUEYA("浅血牙", "#eacdd1"),
    QIANZONGHUI("浅棕灰", "#e1dbcd"),
    KAQIHUANG("卡其黄", "#d5b884"),
    KAQILV("卡其绿", "#647370"),
    QIEPIZI("茄皮紫", "#674950"),
    QUEHUI("鹊灰", "#455667"),
    RONGLAN("绒蓝", "#31678d"),
    SANLV("三绿", "#90caaf"),
    SHALV("沙绿", "#005b5a"),
    SHAQING("沙青", "#2b5e7d"),
    SHENYAN("深烟", "#5a4c4c"),
    SHENYANHONG("深烟红", "#643441"),
    SHENZHUYUE("深竹月", "#2578b5"),
    SHIYANGJIN("十样锦", "#fcb1aa"),
    SHUIDIAOHUI("水貂灰", "#949c97"),
    SHUIHUANG("水黄", "#bed2b6"),
    TENGHUANG("藤黄", "#f2de76"),
    TIANQINGSE("天青色", "#2ec3e7"),
    TIEHUI("铁灰", "#37444b"),
    TUHUANG("土黄", "#ce9335"),
    XIANGSIHUI("相思灰", "#625c52"),
    XUEHONG("血红", "#a03e28"),
    XINGHONG("猩红", "#c43739"),
    XIONGHUANG("雄黄", "#d0853d"),
    XIONGJING("雄精", "#e47542"),
    XIUHONG("锈红", "#4d1919"),
    XIULV("锈绿", "#b8c8b7"),
    XUANJIN("选金", "#796f54"),
    XUESE("雪色", "#fffafa"),
    XUEZI("雪紫", "#79485a"),
    YADANQING("鸭蛋青", "#d1e3db"),
    YANGCONGZI("洋葱紫", "#9c6680"),
    YANGHONG("洋红", "#dc143c"),
    YANHONG("艳红", "#cc3536"),
    YINGWULV("鹦鹉绿", "#008e59"),
    YINZHU("银朱", "#dd3b44"),
    YOULV("油绿", "#45554a"),
    YOUYANMO("油烟墨", "#3f3f3c"),
    YUANQING("元青", "#3e3c3d"),
    YANZHI("胭脂", "#c03f3c"),
    YINBO("银箔", "#585a57"),
    YUEJIHONG("月季红", "#bb1c33"),
    YUSHI("玉石蓝", "#507883"),
    ZAOHONG("枣红", "#89303f"),
    ZHANGDAN("章丹", "#eb652d"),
    ZHENGHUI("正灰", "#93a2a9"),
    ZHIHUANG("枝黄", "#dbc7a6"),
    ZHIJINHUI("织锦灰", "#748a8d"),
    ZHIZONG("纸棕", "#bca590"),
    ZHONGZONGHUI("中棕灰", "#a9987c"),
    ZIFEN("紫粉", "#a54358"),
    ZISHUIJING("紫水晶", "#c3a6cb"),
    ZITENGHUI("紫藤灰", "#857e95"),
    ZIWEIHUA("紫薇花", "#eea5d1"),
    ZONGCHA("棕茶", "#b8844f"),

    // 中国传统色彩标准色卡
    JINGBAI("精白", "#ffffff"),
    YINBAI("银白", "#e9e7ef"),
    QIANBAI("铅白", "#f0f0f4"),
    SHUANGSE("霜色", "#e9f1f6"),
    XUEBAI("雪白", "#f0fcff"),
    YINGBAI("莹白", "#e3f9fd"),
    YUEBAI("月白", "#d6ecf0"),
    XIANGYABAI("象牙白", "#fffbf0"),
    GAO("缟", "#f2ecde"),
    YUDUBAI("鱼肚白", "#fcefe8"),
    BAI_FEN("白粉", "#fff2fd"),
    CHABAI("茶白", "#f3f9f1"),
    YALUANQING("鸭卵青", "#e0eee8"),
    SU("素", "#e0f0e9"),
    QINGBAI("青白", "#c0ebd7"),
    XIEKEQING("蟹壳青", "#bbcdc5"),
    HUABAI("花白", "#c2ccd0"),
    LAOYIN("老银", "#bacac6"),
    HUISE("灰色", "#808080"),
    CANGSE("苍色", "#75878a"),
    SHUISE("水色", "#88ada6"),
    YOU("黝", "#6b6882"),
    WUSE("乌色", "#725e82"),
    XUANQING("玄青", "#3d3b4f"),
    WUHEI("乌黑", "#392f41"),
    LI("黎", "#75664d"),
    LI2("黧", "#5d513c"),
    YOUHEI("黝黑", "#665757"),
    ZISE("缁色", "#493131"),
    MEIHEI("煤黑", "#312520"),
    QIHEI("漆黑", "#161823"),
    HEISE("黑色", "#000000"),
    YINGCAOSE("樱草色", "#eaff56"),
    EHUANG("鹅黄", "#fff143"),
    YAHUANG("鸭黄", "#faff72"),
    XINGHUANG("杏黄", "#ffa631"),
    CHENGHUANG("橙黄", "#ffa400"),
    CHENGSE("橙色", "#fa8c35"),
    XINGHONG2("杏红", "#ff8c31"),
    JUHONG3("橘黄", "#ff8936"),
    JUHONG4("橘红", "#ff7500"),
    TENGHUANG2("藤黄", "#ffb61e"),
    JIANGHUANG2("姜黄", "#ffc773"),
    CIHUANG("雌黄", "#ffc64b"),
    CHIJIN("赤金", "#f2be45"),
    XIANGSE("缃色", "#f0c239"),
    XIONGHUANG2("雄黄", "#e9bb1d"),
    QIUXIANGSE("秋香色", "#d9b611"),
    JINSE("金色", "#eacd76"),
    YASE("牙色", "#eedeb0"),
    KUHUANG("枯黄", "#d3b17d"),
    HUANGLU("黄栌", "#e29c45"),
    WUJIN("乌金", "#a78e44"),
    HUNHUANG("昏黄", "#c89b40"),
    ZONGHUANG("棕黄", "#ae7000"),
    HUPO("琥珀", "#ca6924"),
    ZONGSE("棕色", "#b25d25"),
    CHASE("茶色", "#b35c44"),
    ZONGHONG("棕红", "#9b4400"),
    ZHE("赭", "#9c5333"),
    TUOSE("驼色", "#a88462"),
    QIUSE("秋色", "#896c39"),
    ZONGLV("棕绿", "#827100"),
    HESE("褐色", "#6e511e"),
    ZONGHEI("棕黑", "#7c4b00"),
    ZHESE("赭色", "#955539"),
    ZHESHI("赭石", "#845a33"),
    SONGHUASE("松花色", "#bce672"),
    LIUHUANG("柳黄", "#c9dd22"),
    NENLV("嫩绿", "#bddd22"),
    LIULV("柳绿", "#afdd22"),
    CONGHUANG("葱黄", "#a3d900"),
    CONGLV("葱绿", "#9ed900"),
    DOULV("豆绿", "#9ed048"),
    DOUQING("豆青", "#96ce54"),
    YOULV2("油绿", "#00bc12"),
    CONGQING("葱青", "#0eb83a"),
    QINGCONG("青葱", "#0aa344"),
    SHILV("石绿", "#16a951"),
    SONGBO_LV("松柏绿", "#21a675"),
    SONGHUA_LV("松花绿", "#057748"),
    LV_SHEN("绿沈", "#0c8918"),
    LVSE("绿色", "#00e500"),
    CAOLV("草绿", "#40de5a"),
    QINGCUI("青翠", "#00e079"),
    QINGSE("青色", "#00e09e"),
    FEICUI_LV("翡翠绿", "#3de1ad"),
    BILV("碧绿", "#2add9c"),
    YUSE("玉色", "#2edfa3"),
    PIAO("缥", "#7fecad"),
    AILV("艾绿", "#a4e2c6"),
    SHILV2("石绿", "#7bcfa6"),
    BISE("碧色", "#1bd1a5"),
    QINGBI("青碧", "#48c0a3"),
    TONGLV("铜绿", "#549688"),
    ZHUQING("竹青", "#789262"),
    MOHUI("墨灰", "#758a99"),
    MOSE("墨色", "#50616d"),
    YAQING("鸦青", "#424c50"),
    AN("黯", "#41555d"),
    ZHUSHA("朱砂", "#ff461f"),
    HUOHONG("火红", "#ff2d51"),
    ZHU_BIAO("朱磦", "#f36838"),
    FEISE("妃色", "#ed5736"),
    YANGFEN("洋粉", "#ff4777"),
    PINHONG2("品红", "#f00056"),
    FENHONG("粉红", "#ffb3a7"),
    TAOHONG("桃红", "#f47983"),
    HAITANGHONG("海棠红", "#db5a6b"),
    YINGTAOSE("樱桃色", "#c93756"),
    TUOYAN("酡颜", "#f9906f"),
    YINHONG("银红", "#f05654"),
    DAHONG("大红", "#ff2121"),
    SHILIUHONG("石榴红", "#f20c00"),
    JIANGZI2("绛紫", "#8c4356"),
    FEIHONG2("绯红", "#c83c23"),
    ZHUHONG("朱红", "#ff4c00"),
    DAN("丹", "#ff4e20"),
    TONG("彤", "#f35336"),
    TUOHONG("酡红", "#dc3023"),
    YAN("炎", "#ff3300"),
    QIANSE("茜色", "#cb3a56"),
    WAN("绾", "#a98175"),
    TAN("檀", "#b36d61"),
    YANHONG2("嫣红", "#ef7a82"),
    YANGHONG2("洋红", "#ff0097"),
    ZAOHONG2("枣红", "#c32136"),
    YANHONG3("殷红", "#be0027"),
    HECHI("赫赤", "#c91f37"),
    YINZHU2("银朱", "#bf242a"),
    CHI("赤", "#c3272b"),
    YANZHI2("胭脂", "#9d2933"),
    LISE("栗色", "#60281e"),
    XUANSE("玄色", "#622a1d"),
    WEILAN("蔚蓝", "#70f3ff"),
    LAN("蓝", "#44cef6"),
    BILAN("碧蓝", "#3eede7"),
    SHIQING("石青", "#1685a9"),
    DIANQING("靛青", "#177cb0"),
    DIANLAN2("靛蓝", "#065279"),
    HUAQING2("花青", "#003472"),
    BAOLAN2("宝蓝", "#4b5cc4"),
    LANHUISE("蓝灰色", "#a1afc9"),
    ZANGQING("藏青", "#2e4e7e"),
    ZANGLAN2("藏蓝", "#3b2e7e"),
    DAI("黛", "#4a4266"),
    DAILV("黛绿", "#426666"),
    DAILAN2("黛蓝", "#425066"),
    DAIZI("黛紫", "#574266"),
    ZISE2("紫色", "#8d4bbb"),
    ZIJIANG("紫酱", "#815463"),
    JIANGZI3("酱紫", "#815476"),
    ZITAN("紫檀", "#4c221b"),
    GANQING("绀青", "#003371"),
    ZITANG("紫棠", "#56004f"),
    QINGLIAN("青莲", "#801dae"),
    QUNQING("群青", "#4c8dae"),
    XUEQING("雪青", "#b0a4e3"),
    DINGXIANGSE("丁香色", "#cca4e3"),
    OUSE("藕色", "#edd1d8"),
    OUHE_SE("藕合色", "#e4c6d0");

    private final String chineseName;
    private final String hexCode;

    ChineseColors(String chineseName, String hexCode) {
        this.chineseName = chineseName;
        this.hexCode = hexCode;
    }

    public String getMinecraftHexFormat() {
        // 转换为Minecraft十六进制格式: §x§R§R§G§G§B§B
        String cleanHex = hexCode.replace("#", "");
        if (cleanHex.length() != 6) {
            return "";
        }

        StringBuilder builder = new StringBuilder("§x");
        for (char c : cleanHex.toCharArray()) {
            builder.append('§').append(Character.toLowerCase(c));
        }
        return builder.toString();
    }

    /**
     * 根据中文名称获取颜色
     */
    public static ChineseColors getByName(String name) {
        for (ChineseColors color : values()) {
            if (color.chineseName.equals(name)) {
                return color;
            }
        }
        return null;
    }

    /**
     * 根据十六进制代码获取颜色
     */
    public static ChineseColors getByHex(String hex) {
        String targetHex = hex.startsWith("#") ? hex : "#" + hex;
        for (ChineseColors color : values()) {
            if (color.hexCode.equalsIgnoreCase(targetHex)) {
                return color;
            }
        }
        return null;
    }

    /**
     * 获取随机中国传统颜色
     */
    public static ChineseColors getRandom() {
        ChineseColors[] colors = values();
        return colors[(int) (Math.random() * colors.length)];
    }

    @Override
    public String toString() {
        return chineseName + " (" + hexCode + ")";
    }
}
