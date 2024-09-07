package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class LemmaProcessor {

    public HashMap<String, Integer> countLemmas(String text) {
        String cleanText = clearText(text);
        if (cleanText.isEmpty()) {
            return new HashMap<>();
        }
        String[] words = cleanText.toLowerCase().split("[\\s\\p{Punct}]+");
        HashMap<String, Integer> occurrences = new HashMap<>();
        for (String word : words) {
            if (!isServiceWord(word)) {
                List<String> baseForms = findBaseForms(word);
                for (String form : baseForms) {
                    if (occurrences.containsKey(form)) {
                        occurrences.put(form, occurrences.get(form) + 1);
                    } else {
                        occurrences.put(form, 1);
                    }
                }
            }
        }
        return occurrences;
    }


    private List<String> findBaseForms(String word) {
        try {
            LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
            return luceneMorphology.getNormalForms(word);
        } catch (IOException e) {
            System.out.println("findBaseForms exception");
            e.printStackTrace();
            return null;
        }
    }


    public boolean isServiceWord(String word) {
        try {
            LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
            List<String> wordBaseFormsInfo = luceneMorphology.getMorphInfo(word);
            for (String info : wordBaseFormsInfo) {
                if (info.contains("СОЮЗ") ||
                        info.contains("ЧАСТ") ||
                        info.contains("МЕЖД") ||
                        info.contains("ПРЕДЛ") ||
                        info.contains("МС")) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            System.out.println("isServiceWord exception");
            e.printStackTrace();
            return false;
        }
    }


    private String clearText(String text) {
        String textWithoutScripts = text.replaceAll("(?is)<script[^>]*>.*?</script>", "");
        String textWithoutTags = textWithoutScripts.replaceAll("<[^>]*>", "");
        String textWithoutNbsp = textWithoutTags.replaceAll("&nbsp;", " ").replaceAll("&nbsp", " ");
        String textWithoutEmptyLines = textWithoutNbsp.replaceAll("(?m)^\\s*$(\r?\n)?", "");
        String textWithoutSpecialChars = textWithoutEmptyLines.replaceAll("[\\p{So}\\p{Cntrl}]+", "");
        String textWithoutDigits = textWithoutSpecialChars.replaceAll("\\d+", "");  // Удаление цифр
        String textWithoutEnglishWords = textWithoutDigits.replaceAll("\\b[a-zA-Z]+\\b", "");  // Удаление англ. слов
        return textWithoutEnglishWords.trim();
    }


    public static void main(String[] args) {
        String text = "<!doctype html>\n" +
                " <html>\n" +
                "  <head>\n" +
                "   <title>Интернет-магазин PlayBack.ru</title>\n" +
                "   <meta name=\"description\" content=\"Продажа по доступным ценам. PlayBack.ru - Интернет-Магазин - Большой выбор смартфонов, планшетов, носимой электроники по низким ценам, отличный сервис, гарантии производителя\">\n" +
                "   <meta name=\"keywords\" content=\"купить, цена, описание, интернет-магазин, интернет, магазин, продажа, смартфоны\">\n" +
                "   <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
                "   <meta http-equiv=\"Last-Modified\" content=\"Sat, 30 Oct 2021 15:19:33 GMT\">\n" +
                "   <link rel=\"shortcut icon\" href=\"/favicon.ico\">\n" +
                "   <link rel=\"apple-touch-icon\" href=\"/logo_apple.png\">\n" +
                "   <link rel=\"StyleSheet\" href=\"/include_new/styles.css\" type=\"text/css\" media=\"all\">\n" +
                "   <link rel=\"stylesheet\" href=\"/include_new/jquery-ui.css\">\n" +
                "   <script src=\"https://code.jquery.com/jquery-1.8.3.js\"></script>\n" +
                "   <script src=\"https://code.jquery.com/ui/1.10.0/jquery-ui.js\"></script>\n" +
                "   <script src=\"/jscripts/jquery.inputmask.js\" type=\"text/javascript\"></script>\n" +
                "   <script src=\"/jscripts/jquery.inputmask.extensions.js\" type=\"text/javascript\"></script>\n" +
                "   <script src=\"/jscripts/jquery.inputmask.numeric.extensions.js\" type=\"text/javascript\"></script>\n" +
                "   <link rel=\"stylesheet\" type=\"text/css\" href=\"/fancybox/jquery.fancybox-1.3.4.css\" media=\"screen\">\n" +
                "   <script type=\"text/javascript\" src=\"/fancybox/jquery.mousewheel-3.0.4.pack.js\"></script>\n" +
                "   <script type=\"text/javascript\" src=\"/fancybox/jquery.fancybox-1.3.4.js\"></script>\n" +
                "   <script type=\"text/javascript\" src=\"/include_new/playback.js\"></script>\n" +
                "   <script>\n" +
                "   $( function() {\n" +
                "     $( \"#accordion\" ).accordion({\n" +
                "       heightStyle: \"content\",\n" +
                " \t  collapsible: true,\n" +
                " \t  active : false,\n" +
                " \t  activate: function( event, ui ) {\n" +
                "          if ($(ui.newHeader).offset() != null) {\n" +
                "         ui.newHeader,\n" +
                "         $(\"html, body\").animate({scrollTop: ($(ui.newHeader).offset().top)}, 500);\n" +
                "       }\n" +
                "     }\n" +
                "     });\n" +
                " \t} );\n" +
                " \t$( function() {\n" +
                "     var icons = {\n" +
                "       header: \"ui-icon-circle-arrow-e\",\n" +
                "       activeHeader: \"ui-icon-circle-arrow-s\"\n" +
                "     };\n" +
                "     $( \"#accordion\" ).accordion({\n" +
                "       icons: icons\n" +
                "     });\n" +
                "     $( \"#toggle\" ).button().on( \"click\", function() {\n" +
                "       if ( $( \"#accordion\" ).accordion( \"option\", \"icons\" ) ) {\n" +
                "         $( \"#accordion\" ).accordion( \"option\", \"icons\", null );\n" +
                "       } else {\n" +
                "         $( \"#accordion\" ).accordion( \"option\", \"icons\", icons );\n" +
                "       }\n" +
                "     });\n" +
                "   } );\n" +
                "   </script>\n" +
                "   <script type=\"text/javascript\">\n" +
                "   $(function() {\n" +
                "  \n" +
                " $(window).scroll(function() {\n" +
                "  \n" +
                " if($(this).scrollTop() != 0) {\n" +
                "  \n" +
                " $('#toTop').fadeIn();\n" +
                "  \n" +
                " } else {\n" +
                "  \n" +
                " $('#toTop').fadeOut();\n" +
                "  \n" +
                " }\n" +
                "  \n" +
                " });\n" +
                "  \n" +
                " $('#toTop').click(function() {\n" +
                "  \n" +
                " $('body,html').animate({scrollTop:0},800);\n" +
                "  \n" +
                " });\n" +
                "  \n" +
                " });\n" +
                "  \n" +
                " </script>\n" +
                "  </head>\n" +
                "  <body class=\"body_undertop\" topmargin=\"0\" leftmargin=\"0\" bottommargin=\"0\" rightmargin=\"0\" align=\"center\">\n" +
                "   <table class=\"table1\" style=\"box-shadow:0px 0px 32px #595959; margin:5px auto; \" bgcolor=\"#ffffff\" width=\"1024\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" align=\"center\">\n" +
                "    <tbody>\n" +
                "     <tr>\n" +
                "      <td colspan=\"3\" width=\"1024\">\n" +
                "       <table width=\"100%\" border=\"0\" height=\"110px\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top: 0px; margin-bottom: 0px;\">\n" +
                "        <tbody>\n" +
                "         <tr>\n" +
                "          <td width=\"365px\" rowspan=\"2\" align=\"left\">\n" +
                "           <table width=\"250px\" align=\"left\">\n" +
                "            <tbody>\n" +
                "             <tr>\n" +
                "              <td width=\"60px\" height=\"60px\"><img onclick=\"document.location='http://www.playback.ru';return false\" src=\"/img_new/lolo.png\" class=\"logotip\" alt=\"Playback.ru - фотоаппараты, видеокамеры и аксессуары к ним\" title=\"Playback.ru - фотоаппараты, видеокамеры и аксессуары к ним\"></td>\n" +
                "              <td valign=\"center\" align=\"left\"><a class=\"tele_span\" href=\"/\"><span class=\"tele_span_playback\">PlayBack.ru</span></a><br><span style=\"cursor: pointer;\" onclick=\"document.location='/waytoplayback.html';return false\" class=\"getcallback2\">5 минут от метро ВДНХ</span></td>\n" +
                "             </tr>\n" +
                "            </tbody>\n" +
                "           </table></td>\n" +
                "          <td width=\"3px\" rowspan=\"2\" align=\"center\">&nbsp;</td>\n" +
                "          <td width=\"290px\" rowspan=\"2\">\n" +
                "           <table width=\"215px\" align=\"center\">\n" +
                "            <tbody>\n" +
                "             <tr>\n" +
                "              <td valign=\"center\" align=\"center\"><span class=\"tele_span\"><nobr><a href=\"tel:+74951437771\">8(495)143-77-71</a></nobr></span><span class=\"grrafik\"><nobr><br>пн-пт: c 11 до 20<br>сб-вс: с 11 до 18</nobr></span></td>\n" +
                "             </tr>\n" +
                "            </tbody>\n" +
                "           </table></td>\n" +
                "          <td width=\"3px\" align=\"center\" rowspan=\"2\">&nbsp;</td>\n" +
                "          <td width=\"185px\">\n" +
                "           <table width=\"175px\" align=\"center\">\n" +
                "            <tbody>\n" +
                "             <tr>\n" +
                "              <td valign=\"center\" align=\"center\"><span class=\"blocknamezpom\" style=\"cursor: pointer;\" onclick=\"document.location='/tell_about_the_problem.html';return false\">Возникла проблема?<br>Напишите нам!</span></td>\n" +
                "             </tr>\n" +
                "            </tbody>\n" +
                "           </table> <span class=\"tele_span\"></span></td>\n" +
                "          <td width=\"3px\" align=\"center\">&nbsp;</td>\n" +
                "          <td width=\"179px\">\n" +
                "           <table width=\"175px\" align=\"center\">\n" +
                "            <tbody>\n" +
                "             <tr>\n" +
                "              <td width=\"53px\" height=\"50px\" rowspan=\"2\" align=\"left\"><a href=\"/basket.html\"><img src=\"/img_new/basket.png\" width=\"49px\" border=\"0\"></a></td>\n" +
                "              <td valign=\"bottom\" align=\"left\" height=\"25px\"><a class=\"tele_span2\" href=\"/basket.html\">Корзина</a><br><span class=\"take_me_call\"></span></td>\n" +
                "             </tr>\n" +
                "             <tr>\n" +
                "              <td height=\"10px\" align=\"right\" valign=\"top\"><span class=\"basket_inc_label\" id=\"sosotoyaniekorziny\">пуста</span></td>\n" +
                "             </tr>\n" +
                "            </tbody>\n" +
                "           </table></td>\n" +
                "         </tr>\n" +
                "         <tr>\n" +
                "          <td colspan=\"3\" style=\"text-align: right;\">\n" +
                "           <form action=\"/search.php\" method=\"get\" class=\"izkat\"><input type=\"search\" name=\"search_string\" placeholder=\"поиск\" class=\"ssstring\"> <input type=\"submit\" name=\"\" value=\"Искать\" class=\"iskat\">\n" +
                "           </form></td>\n" +
                "         </tr>\n" +
                "        </tbody>\n" +
                "       </table></td> <!--\t<tr> \n" +
                " \t<td colspan=\"3\" style=\"color: #2556A3; font:17px Roboto-Regular,Helvetica,sans-serif; text-align: center; height: 35px;vertical-align: middle;padding-bottom:10px;\">\n" +
                " \t\t<b>Уважаемые покупатели! C 28 апреля по 8 мая работаем по обычному графику. 9 мая - выходной.</b>\n" +
                " \t</td>\n" +
                "   </tr>--->\n" +
                "     </tr>\n" +
                "     <tr>\n" +
                "      <td colspan=\"3\" style=\"text-align: center;\">\n" +
                "       <nav>\n" +
                "        <ul class=\"topmenu\">\n" +
                "         <li><a href=\"\" class=\"active\" onclick=\"return false;\"><img src=\"/img/imglist.png\" height=\"9px\"> Каталог<span class=\"fa fa-angle-down\"></span></a>\n" +
                "          <ul class=\"submenu\">\n" +
                "           <li><a href=\"/catalog/1652.html\">Чехлы для смартфонов Infinix</a></li>\n" +
                "           <li><a href=\"/catalog/1511.html\">Смартфоны</a></li>\n" +
                "           <li><a href=\"/catalog/1300.html\">Чехлы для смартфонов Xiaomi</a></li>\n" +
                "           <li><a href=\"/catalog/1302.html\">Защитные стекла для смартфонов Xiaomi</a></li>\n" +
                "           <li><a href=\"/catalog/1310.html\">Чехлы для Huawei/Honor</a></li>\n" +
                "           <li><a href=\"/catalog/1308.html\">Чехлы для смартфонов Samsung</a></li>\n" +
                "           <li><a href=\"/catalog/1307.html\">Защитные стекла для смартфонов Samsung</a></li>\n" +
                "           <li><a href=\"/catalog/1141.html\">Планшеты</a></li>\n" +
                "           <li><a href=\"/catalog/1315.html\">Зарядные устройства и кабели</a></li>\n" +
                "           <li><a href=\"/catalog/1329.html\">Держатели для смартфонов</a></li>\n" +
                "           <li><a href=\"/catalog/665.html\">Автодержатели</a></li>\n" +
                "           <li><a href=\"/catalog/1304.html\">Носимая электроника</a></li>\n" +
                "           <li><a href=\"/catalog/1305.html\">Наушники и колонки</a></li>\n" +
                "           <li><a href=\"/catalog/805.html\">Запчасти для телефонов</a></li>\n" +
                "           <li><a href=\"/catalog/1311.html\">Чехлы для планшетов</a></li>\n" +
                "           <li><a href=\"/catalog/1317.html\">Аксессуары для фото-видео</a></li>\n" +
                "           <li><a href=\"/catalog/1318.html\">Чехлы для смартфонов Apple</a></li>\n" +
                "           <li><a href=\"/catalog/1429.html\">USB Флеш-накопители</a></li>\n" +
                "           <li><a href=\"/catalog/1473.html\">Товары для детей</a></li>\n" +
                "           <li><a href=\"/catalog/1507.html\">Защитные стекла для смартфонов Realme</a></li>\n" +
                "           <li><a href=\"/catalog/1508.html\">Чехлы для смартфонов Realme</a></li>\n" +
                "           <li><a href=\"/catalog/18.html\">Карты памяти</a></li>\n" +
                "           <li><a href=\"/catalog/1303.html\">Защитные стекла для планшетов</a></li>\n" +
                "           <li><a href=\"/catalog/1312.html\">Защитные стекла для смартфонов</a></li>\n" +
                "           <li><a href=\"/catalog/1622.html\">Защитные стекла для смартфонов Apple</a></li>\n" +
                "           <li><a href=\"/catalog/1626.html\">Чехлы для смартфонов Vivo</a></li>\n" +
                "           <li><a href=\"/catalog/1636.html\">Чехлы для смартфонов Tecno</a></li>\n" +
                "          </ul></li>\n" +
                "         <li><a href=\"/dostavka.html\">Доставка</a></li>\n" +
                "         <li><a href=\"/pickup.html\">Самовывоз</a></li>\n" +
                "         <li><a href=\"/payment.html\">Оплата</a></li>\n" +
                "         <li><a href=\"/warranty.html\">Гарантия и обмен</a></li>\n" +
                "         <li><a href=\"/contacts.html\">Контакты</a></li>\n" +
                "        </ul>\n" +
                "       </nav></td>\n" +
                "     </tr>\n" +
                "     <tr>\n" +
                "      <td colspan=\"3\" valign=\"top\">\n" +
                "       <table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n" +
                "        <tbody>\n" +
                "         <tr>\n" +
                "          <!----<td class=\"menu_full_cell\" width=\"253\">---->\n" +
                "          <td colspan=\"2\" class=\"item_full_cell\">\n" +
                "           <table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"padding-top:15px;\">\n" +
                "            <tbody>\n" +
                "             <tr>\n" +
                "              <td colspan=\"2\">\n" +
                "               <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n" +
                "                <tbody>\n" +
                "                 <tr>\n" +
                "                  <td>\n" +
                "                   <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
                "                    <tbody>\n" +
                "                     <tr>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1125465.html';return false\" src=\"/img/product/200/1125465_1_200.jpg\" alt=\"Изображение товара Смартфон Honor 90 Lite 8/256 ГБ RU, титановый серебристый\" title=\"Описание и характеристики Смартфон Honor 90 Lite 8/256 ГБ RU, титановый серебристый\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1125465.html\" title=\"Описание и характеристики Смартфон Honor 90 Lite 8/256 ГБ RU, титановый серебристый\">Смартфон Honor 90 Lite 8/256 ГБ RU, титановый серебристый</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">16990р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1125465\" onclick=\"addtobasket_w_fancy(1125465)\"><span title=\"Купить Смартфон Honor 90 Lite 8/256 ГБ RU, титановый серебристый\" id=\"buyimg1125465\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1124867.html';return false\" src=\"/img/product/200/1124867_1_200.jpg\" alt=\"Изображение товара Смартфон realme 11 4G 8/256 ГБ RU, черный\" title=\"Описание и характеристики Смартфон realme 11 4G 8/256 ГБ RU, черный\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1124867.html\" title=\"Описание и характеристики Смартфон realme 11 4G 8/256 ГБ RU, черный\">Смартфон realme 11 4G 8/256 ГБ RU, черный</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">15950р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1124867\" onclick=\"addtobasket_w_fancy(1124867)\"><span title=\"Купить Смартфон realme 11 4G 8/256 ГБ RU, черный\" id=\"buyimg1124867\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1125461.html';return false\" src=\"/img/product/200/1125461_1_200.jpg\" alt=\"Изображение товара Смартфон realme 12 4G 8/256 ГБ RU (RMX3871), зеленый малахит\" title=\"Описание и характеристики Смартфон realme 12 4G 8/256 ГБ RU (RMX3871), зеленый малахит\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1125461.html\" title=\"Описание и характеристики Смартфон realme 12 4G 8/256 ГБ RU (RMX3871), зеленый малахит\">Смартфон realme 12 4G 8/256 ГБ RU (RMX3871), зеленый малахит</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">16990р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1125461\" onclick=\"addtobasket_w_fancy(1125461)\"><span title=\"Купить Смартфон realme 12 4G 8/256 ГБ RU (RMX3871), зеленый малахит\" id=\"buyimg1125461\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                     </tr>\n" +
                "                     <tr>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1125508.html';return false\" src=\"/img/product/200/1125508_1_200.jpg\" alt=\"Изображение товара Смартфон realme 12 4G 8/512 ГБ RU (RMX3871), голубой рассвет\" title=\"Описание и характеристики Смартфон realme 12 4G 8/512 ГБ RU (RMX3871), голубой рассвет\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1125508.html\" title=\"Описание и характеристики Смартфон realme 12 4G 8/512 ГБ RU (RMX3871), голубой рассвет\">Смартфон realme 12 4G 8/512 ГБ RU (RMX3871), голубой рассвет</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">20250р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1125508\" onclick=\"addtobasket_w_fancy(1125508)\"><span title=\"Купить Смартфон realme 12 4G 8/512 ГБ RU (RMX3871), голубой рассвет\" id=\"buyimg1125508\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1125026.html';return false\" src=\"/img/product/200/1125026_1_200.jpg\" alt=\"Изображение товара Смартфон realme C67 8/256 ГБ Черный камень (РСТ)\" title=\"Описание и характеристики Смартфон realme C67 8/256 ГБ Черный камень (РСТ)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1125026.html\" title=\"Описание и характеристики Смартфон realme C67 8/256 ГБ Черный камень (РСТ)\">Смартфон realme C67 8/256 ГБ Черный камень (РСТ)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">14850р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1125026\" onclick=\"addtobasket_w_fancy(1125026)\"><span title=\"Купить Смартфон realme C67 8/256 ГБ Черный камень (РСТ)\" id=\"buyimg1125026\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1125107.html';return false\" src=\"/img/product/200/1125107_1_200.jpg\" alt=\"Изображение товара Смартфон Samsung Galaxy A05s 4/128 ГБ, Global, черный\" title=\"Описание и характеристики Смартфон Samsung Galaxy A05s 4/128 ГБ, Global, черный\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1125107.html\" title=\"Описание и характеристики Смартфон Samsung Galaxy A05s 4/128 ГБ, Global, черный\">Смартфон Samsung Galaxy A05s 4/128 ГБ, Global, черный</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">9990р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1125107\" onclick=\"addtobasket_w_fancy(1125107)\"><span title=\"Купить Смартфон Samsung Galaxy A05s 4/128 ГБ, Global, черный\" id=\"buyimg1125107\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                     </tr>\n" +
                "                     <tr>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1125115.html';return false\" src=\"/img/product/200/1125115_1_200.jpg\" alt=\"Изображение товара Смартфон Samsung Galaxy A05s 6/128 ГБ, Global, черный\" title=\"Описание и характеристики Смартфон Samsung Galaxy A05s 6/128 ГБ, Global, черный\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1125115.html\" title=\"Описание и характеристики Смартфон Samsung Galaxy A05s 6/128 ГБ, Global, черный\">Смартфон Samsung Galaxy A05s 6/128 ГБ, Global, черный</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">11600р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1125115\" onclick=\"addtobasket_w_fancy(1125115)\"><span title=\"Купить Смартфон Samsung Galaxy A05s 6/128 ГБ, Global, черный\" id=\"buyimg1125115\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1125179.html';return false\" src=\"/img/product/200/1125179_1_200.jpg\" alt=\"Изображение товара Смартфон Samsung Galaxy A35 5G 8/256 ГБ темно-синий (Global Version)\" title=\"Описание и характеристики Смартфон Samsung Galaxy A35 5G 8/256 ГБ темно-синий (Global Version)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1125179.html\" title=\"Описание и характеристики Смартфон Samsung Galaxy A35 5G 8/256 ГБ темно-синий (Global Version)\">Смартфон Samsung Galaxy A35 5G 8/256 ГБ темно-синий (Global Version)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">23700р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1125179\" onclick=\"addtobasket_w_fancy(1125179)\"><span title=\"Купить Смартфон Samsung Galaxy A35 5G 8/256 ГБ темно-синий (Global Version)\" id=\"buyimg1125179\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1125199.html';return false\" src=\"/img/product/200/1125199_1_200.jpg\" alt=\"Изображение товара Смартфон Samsung Galaxy A55 5G 8/256 ГБ темно-синий (Global Version)\" title=\"Описание и характеристики Смартфон Samsung Galaxy A55 5G 8/256 ГБ темно-синий (Global Version)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1125199.html\" title=\"Описание и характеристики Смартфон Samsung Galaxy A55 5G 8/256 ГБ темно-синий (Global Version)\">Смартфон Samsung Galaxy A55 5G 8/256 ГБ темно-синий (Global Version)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">29600р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1125199\" onclick=\"addtobasket_w_fancy(1125199)\"><span title=\"Купить Смартфон Samsung Galaxy A55 5G 8/256 ГБ темно-синий (Global Version)\" id=\"buyimg1125199\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                     </tr>\n" +
                "                     <tr>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1124759.html';return false\" src=\"/img/product/200/1124759_1_200.jpg\" alt=\"Изображение товара Смартфон Xiaomi 13T 12/256 ГБ черный  (Global Version)\" title=\"Описание и характеристики Смартфон Xiaomi 13T 12/256 ГБ черный  (Global Version)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1124759.html\" title=\"Описание и характеристики Смартфон Xiaomi 13T 12/256 ГБ черный  (Global Version)\">Смартфон Xiaomi 13T 12/256 ГБ черный (Global Version)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">36200р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1124759\" onclick=\"addtobasket_w_fancy(1124759)\"><span title=\"Купить Смартфон Xiaomi 13T 12/256 ГБ черный  (Global Version)\" id=\"buyimg1124759\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1124768.html';return false\" src=\"/img/product/200/1124768_1_200.jpg\" alt=\"Изображение товара Смартфон Xiaomi 13T 8/256 ГБ черный (РСТ)\" title=\"Описание и характеристики Смартфон Xiaomi 13T 8/256 ГБ черный (РСТ)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1124768.html\" title=\"Описание и характеристики Смартфон Xiaomi 13T 8/256 ГБ черный (РСТ)\">Смартфон Xiaomi 13T 8/256 ГБ черный (РСТ)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">33550р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1124768\" onclick=\"addtobasket_w_fancy(1124768)\"><span title=\"Купить Смартфон Xiaomi 13T 8/256 ГБ черный (РСТ)\" id=\"buyimg1124768\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1124828.html';return false\" src=\"/img/product/200/1124828_1_200.jpg\" alt=\"Изображение товара Смартфон Xiaomi Poco C65 8/256 ГБ черный (РСТ)\" title=\"Описание и характеристики Смартфон Xiaomi Poco C65 8/256 ГБ черный (РСТ)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1124828.html\" title=\"Описание и характеристики Смартфон Xiaomi Poco C65 8/256 ГБ черный (РСТ)\">Смартфон Xiaomi Poco C65 8/256 ГБ черный (РСТ)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">10300р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1124828\" onclick=\"addtobasket_w_fancy(1124828)\"><span title=\"Купить Смартфон Xiaomi Poco C65 8/256 ГБ черный (РСТ)\" id=\"buyimg1124828\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                     </tr>\n" +
                "                     <tr>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1125483.html';return false\" src=\"/img/product/200/1125483_1_200.jpg\" alt=\"Изображение товара Смартфон Xiaomi Poco M6 8/256 ГБ Черный (Global Version)\" title=\"Описание и характеристики Смартфон Xiaomi Poco M6 8/256 ГБ Черный (Global Version)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1125483.html\" title=\"Описание и характеристики Смартфон Xiaomi Poco M6 8/256 ГБ Черный (Global Version)\">Смартфон Xiaomi Poco M6 8/256 ГБ Черный (Global Version)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">13290р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1125483\" onclick=\"addtobasket_w_fancy(1125483)\"><span title=\"Купить Смартфон Xiaomi Poco M6 8/256 ГБ Черный (Global Version)\" id=\"buyimg1125483\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1124270.html';return false\" src=\"/img/product/200/1124270_1_200.jpg\" alt=\"Изображение товара Смартфон Xiaomi Redmi 12 8/256 ГБ midnight black (РСТ)\" title=\"Описание и характеристики Смартфон Xiaomi Redmi 12 8/256 ГБ midnight black (РСТ)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1124270.html\" title=\"Описание и характеристики Смартфон Xiaomi Redmi 12 8/256 ГБ midnight black (РСТ)\">Смартфон Xiaomi Redmi 12 8/256 ГБ midnight black (РСТ)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">11950р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1124270\" onclick=\"addtobasket_w_fancy(1124270)\"><span title=\"Купить Смартфон Xiaomi Redmi 12 8/256 ГБ midnight black (РСТ)\" id=\"buyimg1124270\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1124272.html';return false\" src=\"/img/product/200/1124272_1_200.jpg\" alt=\"Изображение товара Смартфон Xiaomi Redmi 12 8/256 ГБ sky blue (РСТ)\" title=\"Описание и характеристики Смартфон Xiaomi Redmi 12 8/256 ГБ sky blue (РСТ)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1124272.html\" title=\"Описание и характеристики Смартфон Xiaomi Redmi 12 8/256 ГБ sky blue (РСТ)\">Смартфон Xiaomi Redmi 12 8/256 ГБ sky blue (РСТ)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">11950р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1124272\" onclick=\"addtobasket_w_fancy(1124272)\"><span title=\"Купить Смартфон Xiaomi Redmi 12 8/256 ГБ sky blue (РСТ)\" id=\"buyimg1124272\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                     </tr>\n" +
                "                     <tr>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1125509.html';return false\" src=\"/img/product/200/1125509_1_200.jpg\" alt=\"Изображение товара Смартфон Xiaomi Redmi 13 8/256 ГБ без NFC Midnight Black (Global Version)\" title=\"Описание и характеристики Смартфон Xiaomi Redmi 13 8/256 ГБ без NFC Midnight Black (Global Version)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1125509.html\" title=\"Описание и характеристики Смартфон Xiaomi Redmi 13 8/256 ГБ без NFC Midnight Black (Global Version)\">Смартфон Xiaomi Redmi 13 8/256 ГБ без NFC Midnight Black (Global Version)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">13650р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1125509\" onclick=\"addtobasket_w_fancy(1125509)\"><span title=\"Купить Смартфон Xiaomi Redmi 13 8/256 ГБ без NFC Midnight Black (Global Version)\" id=\"buyimg1125509\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1125286.html';return false\" src=\"/img/product/200/1125286_1_200.jpg\" alt=\"Изображение товара Смартфон Xiaomi Redmi 13C 4/128 ГБ черный (РСТ)\" title=\"Описание и характеристики Смартфон Xiaomi Redmi 13C 4/128 ГБ черный (РСТ)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1125286.html\" title=\"Описание и характеристики Смартфон Xiaomi Redmi 13C 4/128 ГБ черный (РСТ)\">Смартфон Xiaomi Redmi 13C 4/128 ГБ черный (РСТ)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">9600р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1125286\" onclick=\"addtobasket_w_fancy(1125286)\"><span title=\"Купить Смартфон Xiaomi Redmi 13C 4/128 ГБ черный (РСТ)\" id=\"buyimg1125286\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1125510.html';return false\" src=\"/img/product/200/1125510_1_200.jpg\" alt=\"Изображение товара Смартфон Xiaomi Redmi A3x 3/64 ГБ черный (РСТ)\" title=\"Описание и характеристики Смартфон Xiaomi Redmi A3x 3/64 ГБ черный (РСТ)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1125510.html\" title=\"Описание и характеристики Смартфон Xiaomi Redmi A3x 3/64 ГБ черный (РСТ)\">Смартфон Xiaomi Redmi A3x 3/64 ГБ черный (РСТ)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">6200р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1125510\" onclick=\"addtobasket_w_fancy(1125510)\"><span title=\"Купить Смартфон Xiaomi Redmi A3x 3/64 ГБ черный (РСТ)\" id=\"buyimg1125510\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                     </tr>\n" +
                "                     <tr>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1123982.html';return false\" src=\"/img/product/200/1123982_1_200.jpg\" alt=\"Изображение товара Смартфон Xiaomi Redmi Note 12 Pro 4G 8/256 ГБ голубой (Global Version)\" title=\"Описание и характеристики Смартфон Xiaomi Redmi Note 12 Pro 4G 8/256 ГБ голубой (Global Version)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1123982.html\" title=\"Описание и характеристики Смартфон Xiaomi Redmi Note 12 Pro 4G 8/256 ГБ голубой (Global Version)\">Смартфон Xiaomi Redmi Note 12 Pro 4G 8/256 ГБ голубой (Global Version)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">20200р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1123982\" onclick=\"addtobasket_w_fancy(1123982)\"><span title=\"Купить Смартфон Xiaomi Redmi Note 12 Pro 4G 8/256 ГБ голубой (Global Version)\" id=\"buyimg1123982\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1123979.html';return false\" src=\"/img/product/200/1123979_1_200.jpg\" alt=\"Изображение товара Смартфон Xiaomi Redmi Note 12 Pro 4G 8/256 ГБ голубой (РСТ)\" title=\"Описание и характеристики Смартфон Xiaomi Redmi Note 12 Pro 4G 8/256 ГБ голубой (РСТ)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1123979.html\" title=\"Описание и характеристики Смартфон Xiaomi Redmi Note 12 Pro 4G 8/256 ГБ голубой (РСТ)\">Смартфон Xiaomi Redmi Note 12 Pro 4G 8/256 ГБ голубой (РСТ)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">21200р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1123979\" onclick=\"addtobasket_w_fancy(1123979)\"><span title=\"Купить Смартфон Xiaomi Redmi Note 12 Pro 4G 8/256 ГБ голубой (РСТ)\" id=\"buyimg1123979\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1123981.html';return false\" src=\"/img/product/200/1123981_1_200.jpg\" alt=\"Изображение товара Смартфон Xiaomi Redmi Note 12 Pro 4G 8/256 ГБ серый (Global Version)\" title=\"Описание и характеристики Смартфон Xiaomi Redmi Note 12 Pro 4G 8/256 ГБ серый (Global Version)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1123981.html\" title=\"Описание и характеристики Смартфон Xiaomi Redmi Note 12 Pro 4G 8/256 ГБ серый (Global Version)\">Смартфон Xiaomi Redmi Note 12 Pro 4G 8/256 ГБ серый (Global Version)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">19990р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1123981\" onclick=\"addtobasket_w_fancy(1123981)\"><span title=\"Купить Смартфон Xiaomi Redmi Note 12 Pro 4G 8/256 ГБ серый (Global Version)\" id=\"buyimg1123981\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                     </tr>\n" +
                "                     <tr>\n" +
                "                      <td class=\"catalog_content_cell\" width=\"33%\">\n" +
                "                       <table width=\"250\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 15px;\">\n" +
                "                        <tbody>\n" +
                "                         <tr>\n" +
                "                          <td colspan=\"2\" class=\"item_img_cell\"><img onclick=\"document.location='/product/1125059.html';return false\" src=\"/img/product/200/1125059_1_200.jpg\" alt=\"Изображение товара Смартфон Xiaomi Redmi Note 13 Pro+ 5G 8/256 ГБ Midnight Black (РСТ)\" title=\"Описание и характеристики Смартфон Xiaomi Redmi Note 13 Pro+ 5G 8/256 ГБ Midnight Black (РСТ)\" width=\"200\" style=\"border:none; decoration: none; \"></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td height=\"42\" colspan=\"2\" class=\"catalog_item_label_cell\"><a href=\"/product/1125059.html\" title=\"Описание и характеристики Смартфон Xiaomi Redmi Note 13 Pro+ 5G 8/256 ГБ Midnight Black (РСТ)\">Смартфон Xiaomi Redmi Note 13 Pro+ 5G 8/256 ГБ Midnight Black (РСТ)</a></td>\n" +
                "                         </tr>\n" +
                "                         <tr>\n" +
                "                          <td class=\"price_cell\">28500р.</td>\n" +
                "                          <td class=\"item_full_info\" id=\"text1125059\" onclick=\"addtobasket_w_fancy(1125059)\"><span title=\"Купить Смартфон Xiaomi Redmi Note 13 Pro+ 5G 8/256 ГБ Midnight Black (РСТ)\" id=\"buyimg1125059\" class=\"buybutton\">Купить</span></td>\n" +
                "                         </tr>\n" +
                "                        </tbody>\n" +
                "                       </table></td>\n" +
                "                      <td class=\"clear_spec_cell\" width=\"33%\">&nbsp;</td>\n" +
                "                      <td class=\"clear_spec_cell\" width=\"33%\">&nbsp;</td>\n" +
                "                     </tr>\n" +
                "                    </tbody>\n" +
                "                   </table></td>\n" +
                "                 </tr>\n" +
                "                </tbody>\n" +
                "               </table></td>\n" +
                "             </tr>\n" +
                "            </tbody>\n" +
                "           </table></td>\n" +
                "         </tr>\n" +
                "         <tr>\n" +
                "          <td colspan=\"3\" align=\"center\">\n" +
                "           <div class=\"footer\">\n" +
                "            <div class=\"footer_block\"><span class=\"footer_h1\">Информация</span> <br> <a href=\"/\">Наши спецпредложения</a> <br> <a href=\"/dostavka.html\">Доставка</a> <br> <a href=\"/payment.html\">Оплата</a> <br> <a href=\"/warranty.html\">Гарантия</a> <br> <a href=\"/contacts.html\">Контакты</a> <br> <a href=\"/privacy_policy.html\">Положение о конфиденциальности и защите персональных данных</a>\n" +
                "            </div>\n" +
                "            <div class=\"footer_block_cont\"><span class=\"footer_tel\">+7(495)143-77-71</span> <br><br> <a class=\"footer_email\" href=\"http://vk.com/playback_ru\" target=\"_blank\"><img src=\"/img/VK.png\" title=\"Наша страница Вконтакте\"></a> &nbsp;&nbsp; <br><br>\n" +
                "            </div>\n" +
                "            <div class=\"footer_block_cont\" style=\"width:260px;\"><span class=\"footer_h1\">График работы:</span> <br> пн-пт: c 11-00 до 20-00 <br> сб-вс: с 11-00 до 18-00 <br><br> <span class=\"footer_h1\">Наш адрес:</span> <br> Москва, Звездный бульвар, 10, <br> строение 1, 2 этаж, офис 10.\n" +
                "            </div>\n" +
                "            <div class=\"footer_block\">\n" +
                "            </div>\n" +
                "            <div class=\"footer_block\">\n" +
                "             <script type=\"text/javascript\" src=\"//vk.com/js/api/openapi.js?105\"></script>\n" +
                "             <div id=\"vk_groups\"></div>\n" +
                "             <script type=\"text/javascript\">\n" +
                " VK.Widgets.Group(\"vk_groups\", {mode: 0, width: \"260\", height: \"210\", color1: 'FFFFFF', color2: '0C5696', color3: '0064BA'}, 48023501);\n" +
                " </script>\n" +
                "            </div>\n" +
                "           </div>\n" +
                "           <div style=\"width: 1024px; font-family: Roboto-Regular,Helvetica,sans-serif; text-align: right; font-size: 12px; text-align: left; padding-left: 10px; color: #595959; background: url(/img/footer-fon.png) repeat;\">\n" +
                "            2005-2024 ©Интернет магазин PlayBack.ru\n" +
                "           </div> <!-- Yandex.Metrika counter --> <script type=\"text/javascript\">\n" +
                "    (function(m,e,t,r,i,k,a){m[i]=m[i]||function(){(m[i].a=m[i].a||[]).push(arguments)};\n" +
                "    m[i].l=1*new Date();k=e.createElement(t),a=e.getElementsByTagName(t)[0],k.async=1,k.src=r,a.parentNode.insertBefore(k,a)})\n" +
                "    (window, document, \"script\", \"https://mc.yandex.ru/metrika/tag.js\", \"ym\");\n" +
                " \n" +
                "    ym(232370, \"init\", {\n" +
                "         clickmap:true,\n" +
                "         trackLinks:true,\n" +
                "         accurateTrackBounce:true,\n" +
                "         webvisor:true\n" +
                "    });\n" +
                " </script>\n" +
                "           <noscript>\n" +
                "            <div>\n" +
                "             <img src=\"https://mc.yandex.ru/watch/232370\" style=\"position:absolute; left:-9999px;\" alt=\"\">\n" +
                "            </div>\n" +
                "           </noscript> <!-- /Yandex.Metrika counter --> <!-- BEGIN JIVOSITE CODE {literal} --> <script type=\"text/javascript\">\n" +
                " (function(){ var widget_id = '8LKJc6dMce';var d=document;var w=window;function l(){\n" +
                "   var s = document.createElement('script'); s.type = 'text/javascript'; s.async = true;\n" +
                "   s.src = '//code.jivosite.com/script/widget/'+widget_id\n" +
                "     ; var ss = document.getElementsByTagName('script')[0]; ss.parentNode.insertBefore(s, ss);}\n" +
                "   if(d.readyState=='complete'){l();}else{if(w.attachEvent){w.attachEvent('onload',l);}\n" +
                "   else{w.addEventListener('load',l,false);}}})();\n" +
                " </script> <!-- {/literal} END JIVOSITE CODE --></td>\n" +
                "         </tr>\n" +
                "        </tbody>\n" +
                "       </table> <a href=\"#\" class=\"scrollup\">Наверх</a></td>\n" +
                "     </tr>\n" +
                "    </tbody>\n" +
                "   </table>\n" +
                "  </body>\n" +
                " </html>";
        LemmaProcessor lemmaProcessor = new LemmaProcessor();
        HashMap<String, Integer> hm = lemmaProcessor.countLemmas(text);
        for (String key : hm.keySet()) {
            System.out.println(key + " - " + hm.get(key));
        }
    }
}


//TODO: обработать
