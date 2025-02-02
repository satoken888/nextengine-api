package jp.co.kawakyo.nextengineapi.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Constant {
	public final static String UPLOADED_FOLDER = "upload/";
	public final static String ROOT_PATH = "/";

	public final static String NE_SHOP_CODE_RAKUTEN = "2";
	public final static String NE_SHOP_CODE_AMAZON = "3";
	public final static String NE_SHOP_CODE_YAHOO = "4";
	public final static String NE_SHOP_CODE_TEL = "5";
	public final static String NE_SHOP_CODE_OFFICIAL = "6";
	public final static String NE_SHOP_CODE_HONKAN = "7";

	public final static String NE_DIV_SHOP_DM = "DM";
	public final static String NE_DIV_SHOP_HONKAN = "honkan";

	public final static String NE_DIV_OUTPUT_SHIPPING = "1";
	public final static String NE_DIV_OUTPUT_ORDER = "2";

	public static final List<String> COMEBACK_ITEMCODE_LIST;
	static {
		List<String> list = new ArrayList<String>();
		list.add("0288s");
		list.add("0200s");
		list.add("0201s");
		list.add("7404s");
		list.add("7400s");
		list.add("6400");
		COMEBACK_ITEMCODE_LIST = Collections.unmodifiableList(list);
	}

	public final static String JSON_CONVERSION_ITEMQUANTITY = "{"
			+ "\"0100-2-0102-2：お徳用喜多方ラーメン赤箱12食入×2箱 味噌のみ　太麺\":{\"0100-0902：お徳用喜多方ラーメン赤箱12食入 味噌のみ　太麺\":2},"
			+ "\"8085：【送料無料】ラーメン屋さんの餃子5パック＋チャーシューセット【冷凍便】\":{\"8083：ラーメン屋さんのスタミナ餃子5パック【冷凍】 \":1,\"6119：【冷凍】450gチャーシュー\":1},"
			+ "\"0121-12：レンジ麺　12個セット【送料無料】\":{\"0121：喜多方ラーメン　レンジ麺\":12},"
			+ "\"0127：喜多方　赤辛味噌ラーメン2食入\":{\"0127：赤辛味噌ラーメン２食入り\":1},"
			+ "\"0127-6：赤辛味噌ラーメン２食入り×6箱\":{\"0127：赤辛味噌ラーメン２食入り\":6},"
			+ "\"0105：喜多方ラーメン　黄箱5食入（醤油3・味噌2） | 河京 太麺 ちぢれ麺 醤油ラーメン 味噌ラーメン 日本三大ラーメン お取り寄せグルメ ギフト プレゼント\":{\"0105：河京 喜多方ラーメン 黄箱5食入(醤油3食味噌2食)\":1},"
			+ "\"0106：喜多方ラーメン　黄箱3食入（醤油）\":{\"0106：喜多方ラーメン黄箱3食入\":1},"
			+ "\"0140-4：会津地鶏しょうゆラーメン4食入×4箱\":{\"0140：会津地鶏しょうゆラーメン4食入\":4},"
			+ "\"7120-6：濃厚醤油ラーメン2食入×6箱\":{\"7120：濃厚醤油ラーメン2食入\":6},"
			+ "\"0142-12：具付き喜多方ラーメン　1食箱入り（醤油）×12箱\":{\"0142：具付き喜多方ラーメン　1食箱入り（醤油）\":12},"
			+ "\"0143-12：具付き喜多方ラーメン　1食箱入り（味噌）×１２箱\":{\"0143：具付き喜多方ラーメン　1食箱入り（味噌）\":12},"
			+ "\"0147-4：具付き喜多方ラーメン　4食箱入り（醤油）×4\":{\"0147：具付き喜多方ラーメン　4食箱入り（醤油）\":4},"
			+ "\"0146-6：具付き喜多方ラーメン　2食箱入り（醤油）×６箱\":{\"0146：具付き喜多方ラーメン　2食箱入り（醤油）\":6},"
			+ "\"6118-5：チャーシュー炊き込みご飯の素×5箱\":{\"6118：チャーシュー炊き込みご飯の素\":5},"
			+ "\"6235-5：ラーメン屋さんのチャーシューカレー　5箱\":{\"6235：ラーメン屋さんのチャーシューカレー\":5},"
			+ "\"6262：ラーメン屋さんのチャーシューカレーとつや姫のセット（3人前）\":{\"6235：ラーメン屋さんのチャーシューカレー\":3,\"：つや姫\":1},"
			+ "\"0121-6-1：レンジ麺　6個セット\":{\"0121：喜多方ラーメン　レンジ麺\":6},"
			+ "\"0108-3：喜多方ラーメン黄箱10食入×3箱\":{\"0108：喜多方ラーメン黄箱10食入\":3},"
			+ "\"7139：喜多方ラーメンレンジ麺ギフトBOX\":{\"0121：喜多方ラーメン　レンジ麺\":2,\"0148：喜多方ラーメン　レンジ麺（みそ）\":2},"
			+ "\"0108-2：喜多方ラーメン黄箱10食入×2箱\":{\"0108：喜多方ラーメン黄箱10食入\":2},"
			+ "\"0121-4：【送料無料】レンジ麺お試し4個セット\":{\"0121：喜多方ラーメン　レンジ麺\":4},"
			+ "\"8096-2：【送料無料】10食入河京お試しセット×2箱\":{\"0108：喜多方ラーメン黄箱10食入\":2},"
			+ "\"0100-2-0100-2：お徳用喜多方ラーメン赤箱12食入×2箱 醤油16味噌8　太麺\":{\"0100-0900：お徳用喜多方ラーメン赤箱12食入　醤油8味噌4　太麺\":2},"
			+ "\"0900-3-0100-3：お徳用喜多方ラーメン赤箱12食入×3箱 醤油24味噌12　太麺\":{\"0100-0900：お徳用喜多方ラーメン赤箱12食入　醤油8味噌4　太麺\":3},"
			+ "\"8096-3：【送料無料】10食入り河京お試しセット×3箱\":{\"0108：喜多方ラーメン黄箱10食入\":3},"
			+ "\"6071-10：25gチャーシュー×10枚\":{\"6071：25gチャーシュー\":10},"
			+ "\"0100-2-0101-2：お徳用喜多方ラーメン赤箱12食入×2箱 醤油のみ　太麺\":{\"0100-0901：お徳用喜多方ラーメン赤箱12食入 醤油のみ　太麺\":2},"
			+ "\"0900-3-0101-3：お徳用喜多方ラーメン赤箱12食入×3箱 醤油のみ　太麺\":{\"0100-0901：お徳用喜多方ラーメン赤箱12食入 醤油のみ　太麺\":3},"
			+ "\"0900-3-0102-3：お徳用喜多方ラーメン赤箱12食入×3箱 味噌のみ　太麺\":{\"0100-0902：お徳用喜多方ラーメン赤箱12食入 味噌のみ　太麺\":3},"
			+ "\"6119-2：【冷凍】450gチャーシュー2本【送料無料】\":{\"6119：【冷凍】450gチャーシュー\":2},"
			+ "\"6119-3：【冷凍】450gチャーシュー3本【送料無料】\":{\"6119：【冷凍】450gチャーシュー\":3},"
			+ "\"7142：喜多方ラーメン赤箱12食入チャーシュー増しまし(＋チャーシュー12)\":{\"7076-7076：喜多方ラーメン赤箱12食入チャーシュー付 醤油8味噌4\":1,\"6071：25gチャーシュー\":12},"
			+ "\"8092：【送料無料】4食チャーシュー入 河京お試しセット\":{\"0135：喜多方ラーメン黄箱4食入\":1,\"6071：25gチャーシュー\":4},"
			+ "\"gd244：【ネコポス】あご塩お試し3食入\":{\"：あご塩スープ\":3,\"2220：麺単品（太麺）\":3,\"6071：25gチャーシュー\":3},"
			+ "\"gd165：【ネコポス　送料無料】　お試し3食入喜多方ラーメン\":{\"：半生麺105ｇ\":3,\"2120：河京醤油スープ\":3},"
			+ "\"0121-6：河京 喜多方ラーメン レンジ麺 醤油6食 河京 太麺 ちぢれ麺 醤油ラーメン ラーメン\":{\"0121：喜多方ラーメン　レンジ麺\":6},"
			+ "\"0121-6-1：喜多方ラーメン　レンジ麺(醤油）6個セット｜河京 太麺 ちぢれ麺 醤油ラーメン 日本三大ラーメン お取り寄せグルメ ギフト プレゼント\":{\"0121：喜多方ラーメン　レンジ麺\":6},"
			+ "\"0121-12：喜多方ラーメン　レンジ麺12個セット | 河京 太麺 ちぢれ麺 日本三大ラーメン 醤油ラーメン お取り寄せグルメ ギフト プレゼント\":{\"0121：喜多方ラーメン　レンジ麺\":12},"
			+ "\"7142：喜多方ラーメン　赤箱12食入チャーシュー増しまし（＋チャーシュー12）（醤油8・味噌4） | 河京 太麺 ちぢれ麺 醤油ラーメン 味噌ラーメン 日本三大ラーメン お取り寄せグルメ ギフト プレゼント\":{\"7076-7076：喜多方ラーメン赤箱12食入チャーシュー付醤油8味噌4\":1,\"6071：25gチャーシュー\":12},"
			+ "\"0105-4：【まとめ買い送料無料】喜多方ラーメン　黄箱5食入（醤油3・味噌2）×4箱まとめ買い | 河京 太麺 ちぢれ麺 醤油ラーメン 味噌ラーメン 日本三大ラーメン お取り寄せグルメ ギフト プレゼント\":{\"0105-0105：喜多方ラーメン黄箱５食入り 醤油3味噌2\":4},"
			+ "\"0134：喜多方ラーメン　プレミアム厚み4食\":{\"0134：喜多方ラーメンプレミアム厚み4食\":1},"
			+ "\"0140：河京 会津地鶏しょうゆラーメン 4食入 太麺 ちぢれ麺 醤油ラーメン お取り寄せグルメ ギフト プレゼント\":{\"0140：会津地鶏しょうゆラーメン4食入\":1},"
			+ "\"0146：具付き喜多方ラーメン　2食入（醤油）\":{\"0146：具付き喜多方ラーメン　2食箱入り（醤油）\":1},"
			+ "\"0146：河京 具付き喜多方ラーメン 2食箱入(醤油) 太麺 ちぢれ麺 醤油ラーメン 味噌ラーメン お取り寄せグルメ ギフト プレゼント\":{\"0146：具付き喜多方ラーメン　2食箱入り（醤油）\":1},"
			+ "\"0147：具付き喜多方ラーメン4食入（醤油）\":{\"0147：具付き喜多方ラーメン　4食箱入り（醤油）\":1},"
			+ "\"0900-3：河京 喜多方ラーメン お徳用赤箱 12食入×3箱 太麺 ちぢれ麺 醤油ラーメン 味噌ラーメン\":{\"0100-0900：お徳用喜多方ラーメン赤箱12食入 醤油8味噌4　太麺\":3},"
			+ "\"7076：喜多方ラーメン 赤箱12食入チャーシュー付（醤油8・味噌4）\":{\"7076-7076：喜多方ラーメン赤箱12食入チャーシュー付 醤油8味噌4\":1},"
			+ "\"7076：河京 喜多方ラーメン 赤箱 12食入 チャーシュー付 太麺 ちぢれ麺 醤油ラーメン 味噌ラーメン\":{\"7076-7076：喜多方ラーメン赤箱12食入チャーシュー付 醤油8味噌4\":1},"
			+ "\"7193：レンジ麺 4個バラエティセット\":{\"0121：喜多方ラーメン　レンジ麺\":1,\"0148：喜多方ラーメン　レンジ麺（みそ）\":1,\"0153：喜多方ラーメン　レンジ麺　冷やし中華\":1,\"0152：喜多方ラーメン　レンジ麺　一平\":1},"
			+ "\"8096-3：【送料無料】10食入河京お試しセット×3箱\":{\"0108：喜多方ラーメン黄箱10食入\":3},"
			+ "\"8096：【送料無料】10食入河京お試しセット\":{\"0108：喜多方ラーメン黄箱10食入\":1},"
			+ "\"gd544：選べる有名店セット（２箱） あじ庵食堂/喜鈴/了承する 1つめの商品をお選びください:あじ庵食堂 2つめの商品をお選びください:喜鈴  沖縄は別途送料1650円をいただきます。:了承する\":{\"0271：あじ庵食堂 淡麗しじみ潮　4食入\":1,\"0256：喜鈴　黄金醤油ラーメン　4食入\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 喜鈴/あじ庵食堂/了承する 1つめの商品をお選びください:喜鈴 2つめの商品をお選びください:あじ庵食堂  沖縄は別途送料1650円をいただきます。:了承する\":{\"0271：あじ庵食堂 淡麗しじみ潮　4食入\":1,\"0256：喜鈴　黄金醤油ラーメン　4食入\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 食堂はせ川/一平/了承する 1つめの商品をお選びください:食堂はせ川 2つめの商品をお選びください:一平  沖縄は別途送料1650円をいただきます。:了承する 1つめの商品をお選びください:あじ庵食堂 2つめの商品をお選びください:喜鈴 沖縄は別途送料1650円をいただきます。:了承する\":{\"0250：食堂はせ川　淡麗醤油中華そば4食\":1,\"0261：喜多方らーめん一平　4食入\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 一平/食堂はせ川/了承する 1つめの商品をお選びください:一平 2つめの商品をお選びください:食堂はせ川  沖縄は別途送料1650円をいただきます。:了承する\":{\"0250：食堂はせ川　淡麗醤油中華そば4食\":1,\"0261：喜多方らーめん一平　4食入\":1},"
			+ "\"gd544：選べる有名店セット（２箱） あじ庵食堂/食堂はせ川/了承する 1つめの商品をお選びください:あじ庵食堂 2つめの商品をお選びください:食堂はせ川  沖縄は別途送料1650円をいただきます。:了承する\":{\"0250：食堂はせ川　淡麗醤油中華そば4食\":1,\"0271：あじ庵食堂 淡麗しじみ潮　4食入\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 食堂はせ川/あじ庵食堂/了承する 1つめの商品をお選びください:食堂はせ川 2つめの商品をお選びください:あじ庵食堂  沖縄は別途送料1650円をいただきます。:了承する\":{\"0250：食堂はせ川　淡麗醤油中華そば4食\":1,\"0271：あじ庵食堂 淡麗しじみ潮　4食入\":1},"
			+ "\"gd544：選べる有名店セット（２箱） あじ庵食堂/一平/了承する 1つめの商品をお選びください:あじ庵食堂 2つめの商品をお選びください:一平  沖縄は別途送料1650円をいただきます。:了承する\":{\"0271：あじ庵食堂 淡麗しじみ潮　4食入\":1,\"0261：喜多方らーめん一平　4食入\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 一平/あじ庵食堂/了承する 1つめの商品をお選びください:一平 2つめの商品をお選びください:あじ庵食堂  沖縄は別途送料1650円をいただきます。:了承する\":{\"0271：あじ庵食堂 淡麗しじみ潮　4食入\":1,\"0261：喜多方らーめん一平　4食入\":1},"
			+ "\"gd544：選べる有名店セット（２箱） あじ庵食堂/河京（5食ミックス）/了承する 1つめの商品をお選びください:あじ庵食堂 2つめの商品をお選びください:河京（5食ミックス）  沖縄は別途送料1650円をいただきます。:了承する\":{\"0271：あじ庵食堂 淡麗しじみ潮　4食入\":1,\"0205：喜多方ラーメン黄箱５食入り\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 河京（5食ミックス）/あじ庵食堂/了承する 1つめの商品をお選びください:河京（5食ミックス） 2つめの商品をお選びください:あじ庵食堂  沖縄は別途送料1650円をいただきます。:了承する\":{\"0271：あじ庵食堂 淡麗しじみ潮　4食入\":1,\"0205：喜多方ラーメン黄箱５食入り\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 食堂はせ川/喜鈴/了承する 1つめの商品をお選びください:食堂はせ川 2つめの商品をお選びください:喜鈴  沖縄は別途送料1650円をいただきます。:了承する\":{\"0250：食堂はせ川　淡麗醤油中華そば4食\":1,\"0256：喜鈴　黄金醤油ラーメン　4食入\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 喜鈴/食堂はせ川/了承する 1つめの商品をお選びください:喜鈴 2つめの商品をお選びください:食堂はせ川  沖縄は別途送料1650円をいただきます。:了承する\":{\"0250：食堂はせ川　淡麗醤油中華そば4食\":1,\"0256：喜鈴　黄金醤油ラーメン　4食入\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 食堂はせ川/河京（5食ミックス）/了承する 1つめの商品をお選びください:食堂はせ川 2つめの商品をお選びください:河京（5食ミックス）  沖縄は別途送料1650円をいただきます。:了承する\":{\"0250：食堂はせ川　淡麗醤油中華そば4食\":1,\"0205：喜多方ラーメン黄箱５食入り\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 河京（5食ミックス）/食堂はせ川/了承する 1つめの商品をお選びください:河京（5食ミックス） 2つめの商品をお選びください:食堂はせ川  沖縄は別途送料1650円をいただきます。:了承する\":{\"0250：食堂はせ川　淡麗醤油中華そば4食\":1,\"0205：喜多方ラーメン黄箱５食入り\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 一平/喜鈴/了承する 1つめの商品をお選びください:一平 2つめの商品をお選びください:喜鈴  沖縄は別途送料1650円をいただきます。:了承する\":{\"0261：喜多方らーめん一平　4食入\":1,\"0256：喜鈴　黄金醤油ラーメン　4食入\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 喜鈴/一平/了承する 1つめの商品をお選びください:喜鈴 2つめの商品をお選びください:一平  沖縄は別途送料1650円をいただきます。:了承する\":{\"0261：喜多方らーめん一平　4食入\":1,\"0256：喜鈴　黄金醤油ラーメン　4食入\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 一平/河京（5食ミックス）/了承する 1つめの商品をお選びください:一平 2つめの商品をお選びください:河京（5食ミックス）  沖縄は別途送料1650円をいただきます。:了承する\":{\"0261：喜多方らーめん一平　4食入\":1,\"0205：喜多方ラーメン黄箱５食入り\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 河京（5食ミックス）/一平/了承する 1つめの商品をお選びください:河京（5食ミックス） 2つめの商品をお選びください:一平  沖縄は別途送料1650円をいただきます。:了承する\":{\"0261：喜多方らーめん一平　4食入\":1,\"0205：喜多方ラーメン黄箱５食入り\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 喜鈴/河京（5食ミックス）/了承する 1つめの商品をお選びください:喜鈴 2つめの商品をお選びください:河京（5食ミックス）  沖縄は別途送料1650円をいただきます。:了承する\":{\"0256：喜鈴　黄金醤油ラーメン　4食入\":1,\"0205：喜多方ラーメン黄箱５食入り\":1},"
			+ "\"gd544：選べる有名店セット（２箱） 河京（5食ミックス）/喜鈴/了承する 1つめの商品をお選びください:河京（5食ミックス） 2つめの商品をお選びください:喜鈴  沖縄は別途送料1650円をいただきます。:了承する\":{\"0256：喜鈴　黄金醤油ラーメン　4食入\":1,\"0205：喜多方ラーメン黄箱５食入り\":1}"
			+ "}";

	private Constant() {
	}
}
