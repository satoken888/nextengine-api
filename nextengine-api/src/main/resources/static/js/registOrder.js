//注文者と同じを押されたときの転記作業
$("#same_delivery").on("click", function (event) {
  $("#destinationKintoneId").val($("#buyerKintoneId").val());
  $("#input_destination_tel").val($("#input_buyer_tel").val());
  $("#input_destination_zipcode").val($("#input_buyer_zipcode").val());
  $("#input_destination_address1").val($("#input_buyer_address1").val());
  $("#input_destination_address2").val($("#input_buyer_address2").val());
  $("#input_destination_name").val($("#input_buyer_name").val());
  $("#input_destination_furi").val($("#input_buyer_furi").val());

  //送料計算処理を実施
  calcShippingCost();
});

//支払い方法を変更した場合の処理
$("#input_payment").on("change", function (event) {
  if ($(this).val() == "代引") {
    //代引き手数料の再計算処理を行う
    calcCODCharge();
  } else {
    //手数料の金額を初期化する
    $("input[name='commisionPrice']").val(0);
  }
  //請求額の計算処理を行う
  calcBillingPrice();
});

//代引手数料の計算処理
function calcCODCharge() {
  var targetPrice = 0;
  var commisionPrice;
  targetPrice += Number($("input[name='itemAllPrice']").val());
  targetPrice += Number($("input[name='taxPrice']").val());
  targetPrice -= Number($("input[name='usePoint']").val());
  targetPrice += Number($("input[name='otherPrice']").val());

  if (targetPrice >= 100000) {
    commisionPrice = 1100;
  } else if (targetPrice >= 30000) {
    commisionPrice = 660;
  } else if (targetPrice >= 10000) {
    commisionPrice = 440;
  } else {
    commisionPrice = 330;
  }

  $("input[name='commisionPrice']").val(commisionPrice);
}

//商品一覧の＋ボタン押下時の処理定義
$("#item_input_table").on("click", ".addButton", function () {
  if ($(this).hasClass("clicked")) {
    //列削除
    $(this).parent().parent().remove();
    calcItemPriceAndTax();
  } else {
    //行追加
    var appendRowStr =
      "<tr><td><div class='addButton'></div></td><td><input name='itemCode' type='text' class='form-control' /></td><td><input name='itemName' type='text' class='form-control' /></td><td><input name='itemOption' type='text' class='form-control' /></td><td><input name='itemPrice' type='text' class='form-control calcItems' /></td><td><input name='itemCount' type='number' class='form-control calcItems' /></td><td><input name='itemTaxRate' type='text' class='form-control calcItems' /></td><td><input name='subTotal' type='text' class='form-control subTotal'/></td></tr>";
    $("#item_input_table tbody").append(appendRowStr);
    $(this).toggleClass("clicked");
  }
});

//datepickerクラスをもつinputフォームへdatepickerの反映
$("input.datepicker").datepicker({
  dateFormat: "yy/mm/dd",
});
//受注日エリアへのdatepickerの反映（初期値に当日の日付を入れる）
$("#input_order_date").datepicker(
  "setDate",
  //当日の日付を記入
  moment().format("YYYY/MM/DD")
);

//購入者・届け先・商品配達費金額などのボタンの切り替え処理定義
$("input[name='options']").change(function () {
  var division = $(this).val();

  if (division == 1) {
    $("#buyer_area,#destination_area,#information_area").removeClass(
      "d-block d-none"
    );
    $("#buyer_area").addClass("d-block");
    $("#destination_area, #information_area").addClass("d-none");
  } else if (division == 2) {
    $("#buyer_area,#destination_area,#information_area").removeClass(
      "d-block d-none"
    );
    $("#destination_area").addClass("d-block");
    $("#buyer_area, #information_area").addClass("d-none");
  } else {
    $("#buyer_area,#destination_area,#information_area").removeClass(
      "d-block d-none"
    );
    $("#information_area").addClass("d-block");
    $("#buyer_area, #destination_area").addClass("d-none");
  }
});

//郵便番号欄を入力したあとのフォーカスを外したときの処理定義
$("#input_buyer_zipcode").blur(function () {
  searchAddressFromZipcode(1);
});
$("#input_destination_zipcode").blur(function () {
  searchAddressFromZipcode(2);
});
//〒→住所ボタン押下時の定義
$("#append_input_buyer_zipcode").on("click", function () {
  searchAddressFromZipcode(1);
});
$("#append_input_destination_zipcode").on("click", function () {
  searchAddressFromZipcode(2);
});

//郵便番号から住所情報を取得するAPI利用処理
//div=1なら購入者欄への記入
//div=2なら送り先欄への記入
function searchAddressFromZipcode(div) {
  //入力された郵便番号を取得
  var inputZipcodeArea;
  if (div == 1) {
    inputZipcodeArea = $("#input_buyer_zipcode");
  } else if (div == 2) {
    inputZipcodeArea = $("#input_destination_zipcode");
  }

  $.ajax({
    url: "https://zipcloud.ibsnet.co.jp/api/search",
    type: "get",
    datatype: "json",
    data: {
      zipcode: inputZipcodeArea.val().replace(/-/g, ""),
    },
  })
    .done(function (data) {
      var json = JSON.parse(data);
      var targetAddressArea;
      if (div == 1) {
        targetAddressArea = $("#input_buyer_address1");
      } else if (div == 2) {
        targetAddressArea = $("#input_destination_address1");
      } else {
        console.error();
      }

      if (json.status == 200 && json.results != null) {
        //該当の住所が見つかった場合
        var results = json.results;

        if (results.length > 1) {
          //複数件、該当の住所が存在するとき
          $.each(results, function (index, value) {
            var address =
              value.address1 + " " + value.address2 + " " + value.address3;
            $("#modal_select_address").append(
              "<option value='" + address + "'>" + address + "</option>"
            );
          });

          $("#modal_zipcode").modal();
          $("#modal_zipcode_OK_button").on("click", function () {
            var selected_value = $("#modal_select_address").val();
            if (selected_value.length == 1) {
              // var addresses = selected_value[0].split(" ");

              //入力フォームに検索結果を入力
              // $("#input_buyer_pref").val(addresses[0]);
              // $("#input_buyer_municipalities").val(addresses[1]);
              targetAddressArea.val(
                // addresses[0] + addresses[1] + addresses[2]
                selected_value[0]
              );

              //送料計算処理を実施する
              calcShippingCost();
            } else {
              console.log("ごめんなさい一つだけせんたくしてください。");
            }

            $("#modal_zipcode").modal("hide");
          });
        } else if (results.length == 1) {
          //1件だけ該当の住所が存在するとき
          // var pref = results[0].address1;
          // var municipalities = results[0].address2;
          var address1 =
            results[0].address1 + results[0].address2 + results[0].address3;

          //入力フォームに検索結果を入力
          // $("#input_buyer_pref").val(pref);
          // $("#input_buyer_municipalities").val(municipalities);
          targetAddressArea.val(address1);

          //送料計算処理を実施する
          calcShippingCost();
        }
      } else {
        //見つからなかった場合、もしくは通信問題があった場合
        //0件のとき
        targetAddressArea.val("");
        //送料計算処理を実施する
        calcShippingCost();
      }
    })
    .fail(function () {
      console.log("通信失敗");
    });
}

//受注確定ボタン処理
$("#test_tel_send").on("click", function () {
  // callSearchGoodsApi("0100");
  if (buyerInfoChangeFlag) {
    //購入者情報が書き換わってる場合
    $("#buyerInfoChangeFlag").val("1");
  }
  if (destinationInfoChangeFlag) {
    //送り先情報が書き換わってる場合
    $("#destinationInfoChangeFlag").val("1");
  }
  $("#input_remarks").val($("#invoice_write").val());
  $("#registOrderForm").submit();
});

$("#clear_button").on("click", function () {
  location.reload();
});

var itemInfoList;
function callSearchGoodsApi(itemCode, targetTrElem) {
  $.ajax({
    url: "/nextengine-api/searchGoods",
    type: "post",
    datatype: "json",
    data: {
      itemCode: itemCode,
    },
  }).done(function (data) {
    console.log(data);

    //初期化
    itemInfoList = data;
    $("#modal_iteminfo_table tbody").empty();

    if (data.length == 1) {
      var itemInfo = data[0];
      $(targetTrElem)
        .find("[name='itemPrice']")
        .val(Math.round(itemInfo.itemPrice));
      $(targetTrElem).find("[name='itemName']").val(itemInfo.itemName);
      $(targetTrElem).find("[name='itemTaxRate']").val(itemInfo.itemTaxrate);
      //再計算
      calcItemPriceAndTax();
    } else {
      var addRowElem = "";
      data.forEach((itemInfo, index) => {
        addRowElem +=
          "<tr data-iteminfo='" +
          index +
          "'><td>" +
          itemInfo.itemCode +
          "</td><td>" +
          itemInfo.itemName +
          "</td><td>" +
          Math.round(itemInfo.itemPrice) +
          "</td><td>" +
          itemInfo.itemTaxrate +
          "</td></tr>";
      });
      $("#modal_iteminfo_table tbody").append(addRowElem);

      //モーダル内の行に選択されたらマークをつける
      $("#modal_iteminfo_table tbody tr").on("click", function (event) {
        if ($(this).hasClass("selected")) {
          $(this).removeClass("bg-warning selected");
        } else {
          $("#modal_iteminfo_table tbody tr").removeClass(
            "bg-warning selected"
          );
          $(this).addClass("bg-warning selected");
        }
      });
      //ダブルクリック時の処理
      $("#modal_iteminfo_table tbody tr").on("dblclick", function (event) {
        $("#modal_iteminfo_table tbody tr").removeClass("bg-warning selected");
        $(this).addClass("bg-warning selected");
        $("#modal_iteminfo_OK_button").click();
      });

      $("#modal_iteminfo_OK_button").off("click");
      $("#modal_iteminfo_OK_button").on("click", function (event) {
        var itemInfoListIndex = $(
          "#modal_iteminfo_table tbody tr.selected"
        ).data("iteminfo");
        var itemInfo = itemInfoList[itemInfoListIndex];
        $(targetTrElem).find("[name='itemCode']").val(itemInfo.itemCode);
        $(targetTrElem)
          .find("[name='itemPrice']")
          .val(Math.round(itemInfo.itemPrice));
        $(targetTrElem).find("[name='itemName']").val(itemInfo.itemName);
        $(targetTrElem).find("[name='itemTaxRate']").val(itemInfo.itemTaxrate);

        //パラメータを転記したあと、モーダルを閉じる
        $("#modal_iteminfo").modal("hide");

        //再計算
        calcItemPriceAndTax();
      });

      //モーダルの表示
      $("#modal_iteminfo").modal();
      console.log(data.length + "個のデータがありました。");
    }
  });
}

//購入者欄電話番号欄でエンターキーを押された場合の処理
$("#input_buyer_tel").on("keydown", function (e) {
  if (e.keyCode == 13) {
    searchCustomerByTel(0);
    buyerInfoChangeFlag = false;
    return false;
  }
});
//届け先欄電話番号欄でエンターキーを押された場合の処理
$("#input_destination_tel").on("keydown", function (e) {
  if (e.keyCode == 13) {
    searchCustomerByTel(1);
    destinationInfoChangeFlag = false;
    return false;
  }
});
$("#item_input_table tbody").on(
  "keydown",
  "input[name='itemCode']",
  function (e) {
    if (e.keyCode == 13) {
      var itemCode = $(this).val();
      var trElem = $(this).parent().parent();

      //商品検索API呼び出し
      callSearchGoodsApi(itemCode, trElem);

      //クリックした行の追加ボタンが緑＋マークの状態の場合
      if (!trElem.find(".addButton").hasClass("clicked")) {
        //クリックし、次の行を作っておく
        trElem.find(".addButton").click();
      }
      calcItemPriceAndTax();

      //商品数量にカーソルを移動する
      trElem.find("input[name='itemCount']").focus();
    }
  }
);

//電話番号から顧客情報を取得する処理
//NEAPIを使用します。
var cutomerInfoListForBuyer;
function searchCustomerByTel(div) {
  var tel = "";

  if (div == 0) {
    //購入者の場合
    tel = $("#input_buyer_tel").val();
  } else {
    //送り先の場合
    tel = $("#input_destination_tel").val();
  }

  $.ajax({
    url: "/nextengine-api/searchCustomer",
    type: "post",
    datatype: "json",
    data: {
      tel: tel,
      div: div,
    },
  })
    .done(function (data) {
      console.log(data);
      //初期化
      const customerTable = $("#modal_customer_table tbody");
      cutomerInfoListForBuyer = data;
      customerTable.empty();

      //取得した情報分、テーブル行を生成する
      var contentHtml;
      // for (var customerInfo of data) {
      data.forEach((customerInfo, index) => {
        contentHtml +=
          "<tr data-customer='" +
          index +
          "'><td>" +
          transformNullToStr(customerInfo.name) +
          "</td><td>" +
          transformNullToStr(customerInfo.kana) +
          "</td><td>" +
          transformNullToStr(customerInfo.zip_code) +
          "</td><td>" +
          transformNullToStr(customerInfo.address1) +
          "</td><td>" +
          transformNullToStr(customerInfo.address2) +
          "</td></tr>";
      });

      //生成した行を画面に追加
      customerTable.append(contentHtml);

      //モーダル内の行に選択されたらマークをつける
      $("#modal_customer_table tbody tr").on("click", function (event) {
        if ($(this).hasClass("selected")) {
          $(this).removeClass("bg-warning selected");
        } else {
          $("#modal_customer_table tbody tr").removeClass(
            "bg-warning selected"
          );
          $(this).addClass("bg-warning selected");
        }
      });
      //ダブルクリックしたら選択したことにする
      $("#modal_customer_table tbody tr").on("dblclick", function (event) {
        $("#modal_customer_table tbody tr").removeClass("bg-warning selected");
        $(this).addClass("bg-warning selected");
        $("#tel_modal_submit").click();
      });

      //電話番号検索からのモーダルサブミットした際の挙動定義
      $("#tel_modal_submit").off("click");
      $("#tel_modal_submit").on("click", function (event) {
        var customerIndex = $("#modal_customer_table tbody tr.selected").data(
          "customer"
        );
        var customerInfo = cutomerInfoListForBuyer[customerIndex];
        console.log(customerInfo);

        if (div == 0) {
          $("#input_buyer_zipcode").val(customerInfo.zip_code);
          $("#input_buyer_address1").val(customerInfo.address1);
          $("#input_buyer_address2").val(customerInfo.address2);
          $("#input_buyer_name").val(customerInfo.name);
          $("#input_buyer_furi").val(customerInfo.kana);
          $("#memo").val(customerInfo.memo);
          $("#usablePoint").val(customerInfo.usablePoint);
          //初期表示時のポイント数を確保しておく
          //仕様ポイントを変更した場合に対応できるように
          initialStatePoint = customerInfo.usablePoint;
          buyerInfoChangeFlag = true;
          $("#buyerKintoneId").val(customerInfo.id);
        } else if (div == 1) {
          $("#input_destination_zipcode").val(customerInfo.zip_code);
          $("#input_destination_address1").val(customerInfo.address1);
          $("#input_destination_address2").val(customerInfo.address2);
          $("#input_destination_name").val(customerInfo.name);
          $("#input_destination_furi").val(customerInfo.kana);
          $("#destinationKintoneId").val(customerInfo.id);

          //送り先住所を入力した際は送料再計算処理を実施する
          calcShippingCost();
        }
        //モーダルの非表示
        $("#exampleModal").modal("hide");
      });

      //モーダルの表示
      $("#exampleModal").modal();
    })
    .fail(function (data) {
      console.log(data);
    });
}

function transformNullToStr(str) {
  return str == null ? "" : str;
}

$("#item_input_table tbody").on("change", ".calcItems", function () {
  calcItemPriceAndTax();
});

function calcItemPriceAndTax() {
  var itemAllPrice = 0;
  var taxPrice = 0;

  var itemTrElem = $("#item_input_table tbody").find("tr");
  itemTrElem.each(function (index) {
    var itemPrice = Number($(this).find("input[name='itemPrice']").val());
    var itemCount = Number($(this).find("input[name='itemCount']").val());
    //小計を表示
    $(this)
      .find("input[name='subTotal']")
      .val(itemPrice * itemCount);
    //小計金額を合計に加算
    itemAllPrice += itemPrice * itemCount;
    taxPrice += Math.round(
      (itemPrice *
        itemCount *
        Number($(this).find("input[name='itemTaxRate']").val())) /
        100
    );
  });

  $("input[name='itemAllPrice']").val(itemAllPrice);
  $("input[name='taxPrice']").val(taxPrice);
  $("input[name='itemAllPrice_taxInclude']").val(itemAllPrice + taxPrice);
  if ($("#input_payment").val() == "代引") {
    //代引き手数料の計算処理を行う
    calcCODCharge();
  }
  calcBillingPrice();
}

$("#input_postage").on("change", function () {
  if ($(this).val() == 1) {
    //送料有りに変更された場合
    calcShippingCost();
  } else {
    //送料無しに変更された場合
    $("input[name='shippingPrice']").val(0);
    calcBillingPrice();
  }
});

$(".calcBilling").on("change", function () {
  calcBillingPrice();
});
var initialStatePoint;
$("input[name='usePoint']").on("change", function () {
  $("#usablePoint").val(initialStatePoint - $(this).val());
});
function calcBillingPrice() {
  var itemAllPrice = Number($("input[name='itemAllPrice']").val());
  var taxPrice = Number($("input[name='taxPrice']").val());
  var shippingPrice = Number($("input[name='shippingPrice']").val());
  var commisionPrice = Number($("input[name='commisionPrice']").val());
  var usePoint = Number($("input[name='usePoint']").val());
  var otherPrice = Number($("input[name='otherPrice']").val());

  $("#input_total_claim").val(
    itemAllPrice +
      taxPrice +
      shippingPrice +
      commisionPrice -
      usePoint -
      otherPrice
  );
}

//送料計算のための変数・処理記載
var YAMATO_SHIPPING_COSTS = new Map()
  .set("東京都", 880)
  .set("大阪府", 880)
  .set("愛知県", 880)
  .set("福島県", 880)
  .set("北海道", 1200)
  .set("青森県", 880)
  .set("岩手県", 880)
  .set("宮城県", 880)
  .set("秋田県", 880)
  .set("山形県", 880)
  .set("茨城県", 880)
  .set("栃木県", 880)
  .set("群馬県", 880)
  .set("埼玉県", 880)
  .set("千葉県", 880)
  .set("神奈川県", 880)
  .set("新潟県", 880)
  .set("富山県", 880)
  .set("石川県", 880)
  .set("福井県", 880)
  .set("山梨県", 880)
  .set("長野県", 880)
  .set("岐阜県", 880)
  .set("静岡県", 880)
  .set("三重県", 880)
  .set("滋賀県", 880)
  .set("京都府", 880)
  .set("兵庫県", 880)
  .set("奈良県", 880)
  .set("和歌山県", 880)
  .set("鳥取県", 1200)
  .set("島根県", 1200)
  .set("岡山県", 1200)
  .set("広島県", 1200)
  .set("山口県", 1200)
  .set("徳島県", 1200)
  .set("香川県", 1200)
  .set("愛媛県", 1200)
  .set("高知県", 1200)
  .set("福岡県", 1200)
  .set("佐賀県", 1200)
  .set("長崎県", 1200)
  .set("熊本県", 1200)
  .set("大分県", 1200)
  .set("宮崎県", 1200)
  .set("鹿児島県", 1200)
  .set("沖縄県", 2500);

//送料計算処理
function calcShippingCost() {
  var YAMATO_SHIPPING_COSTS_ENTRY = YAMATO_SHIPPING_COSTS.entries();
  //送り先住所を取得
  var address = $("#input_destination_address1").val();
  //住所が入力されている場合
  if (address) {
    for (let [key, value] of YAMATO_SHIPPING_COSTS_ENTRY) {
      //住所に記載の都道府県がどれか捜索
      var reg = new RegExp("^" + key);
      var changed = false;
      if (reg.test(address)) {
        //文字列の頭が合致する都道府県があった場合

        //クール区分になっているか確認
        if ($("#input_cool").val() != 0) {
          //クール区分が冷蔵or冷凍の場合
          //クール便手数料330円を追加する
          value += 330;
        }

        //送料欄に適する送料を入力する
        $("input[name='shippingPrice']").val(value);
        changed = true;
        //合致する場所があった場合は処理を終了する
        break;
      }
    }

    if (!changed) {
      $("input[name='shippingPrice']").val(0);
    }
    //送料を計算したあとに請求情報も更新する
    calcBillingPrice();
  } else {
    //住所欄が空白の場合
    //０を入力して、送料を再計算する。
    $("input[name='shippingPrice']").val(0);
    calcBillingPrice();
  }
}

$("#input_destination_address1").on("change", function () {
  calcShippingCost();
});

var cool_flag = false;
$("#input_cool").on("change", function () {
  const COOL_COST = 330;
  var cool_div = $(this).val();
  var current = $("input[name='shippingPrice']").val();
  var addCost = 0;
  if (cool_div == 0) {
    //常温を選択した場合

    if (!cool_div) {
      //もともと常温だった場合
    } else {
      //もともとクールの状態で常温に変更した場合
      addCost = -1 * COOL_COST;
      cool_flag = false;
    }
  } else if (cool_div == 1 || cool_div == 2) {
    //冷蔵または冷凍を選択した場合

    //もともとクールの状態でなかった場合
    if (!cool_flag) {
      addCost = COOL_COST;
      cool_flag = true;
    } else {
      //もともとクールの状態だった場合
      //何も変更なし
    }
  }

  //現状の金額が0でない場合
  if (current != 0) {
    cost = Number(current) + addCost;
    $("input[name='shippingPrice']").val(cost);
    calcBillingPrice();
  }
});

var buyerInfoChangeFlag = false;
var destinationInfoChangeFlag = false;
//購入者情報に変更があったらフラグをたてる
$(".buyerInputForm").on("change", function () {
  buyerInfoChangeFlag = true;
});
//送り先情報に変更があったらフラグをたてる
$(".destinationInputForm").on("change", function () {
  destinationInfoChangeFlag = true;
});
