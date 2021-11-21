package kirill.com

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.InlineQueryResultArticle
import com.pengrad.telegrambot.request.AnswerInlineQuery
import com.pengrad.telegrambot.request.BaseRequest
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.request.SendMessage

private const val PROCESSING_LABEL = "Processing..."

class Bot {
    private val bot = TelegramBot(System.getenv("BOT_TOKEN"))
    private val opponentWins = mutableListOf("01","12","20")

    fun serve() {
        bot.setUpdatesListener {
            it.forEach(this::process)
            return@setUpdatesListener UpdatesListener.CONFIRMED_UPDATES_ALL
        }
    }

    private fun process(update: Update) {
        val message = update.message()
        val callbackQuery = update.callbackQuery()
        var request: BaseRequest<*, *>? = null
        val inlineQuery = update.inlineQuery()

        if (message?.viaBot() != null && message.viaBot().username().equals("sameGameBot_bot")) {
            val replyMarkup = message.replyMarkup()
            val buttons = replyMarkup.inlineKeyboard()

            val button = buttons[0][0]
            val buttonLabel = button.text()

            if (!buttonLabel.equals(PROCESSING_LABEL)) return

            val chatId = message.chat().id()
            val senderName = message.from().firstName()
            val senderChose = button.callbackData()
            val messageId = message.messageId()

            request = EditMessageText(chatId, messageId, message.text())
                .replyMarkup(
                    InlineKeyboardMarkup(
                        InlineKeyboardButton("✊")
                            .callbackData("$chatId, $senderName,$senderChose,0"),
                        InlineKeyboardButton("✂")
                            .callbackData("$chatId, $senderName,$senderChose,1"),
                        InlineKeyboardButton("\uD83D\uDCC4")
                            .callbackData("$chatId, $senderName,$senderChose,2")
                    )
                )
        } else if (inlineQuery != null) {
            val inlineRock = buildInlineButton("rock","✊ Rock", "0")
            val inlinePaper = buildInlineButton("scissors","✂ Scissors", "1")
            val inlineScissors = buildInlineButton("paper","\uD83D\uDCC4 Paper", "2")

            request = AnswerInlineQuery(inlineQuery.id(),inlineRock, inlinePaper, inlineScissors).cacheTime(1)
        } else if (callbackQuery != null) {
            val data = callbackQuery.data().split(",")
            val (chatId, senderName, senderChose, opponentChose) = data
            val opponentName = callbackQuery.from().firstName()

            request = when {
                (senderChose == opponentChose) ->
                    SendMessage(chatId, "Nobody wins")
                (opponentWins.contains(senderChose + opponentChose)) ->
                    SendMessage(chatId, "$opponentName was beaten by $senderName")
                else ->
                    SendMessage(chatId, "$senderName was beaten by $opponentName")
            }
        }
        if (request != null) {
            bot.execute(request)
        }
    }

    private fun buildInlineButton(id: String, tile: String, callbackData: String)  =
        InlineQueryResultArticle(id, tile, "Ready")
            .replyMarkup(
                InlineKeyboardMarkup(InlineKeyboardButton(PROCESSING_LABEL).callbackData(callbackData))
            )
}