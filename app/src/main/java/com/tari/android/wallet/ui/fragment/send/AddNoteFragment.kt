/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ui.fragment.send

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.core.models.enums.MediaType
import com.giphy.sdk.ui.pagination.GPHContent
import com.giphy.sdk.ui.views.GPHGridCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.dimen.*
import com.tari.android.wallet.R.string.emoji_id_chunk_separator
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.databinding.DialogChooseGifBinding
import com.tari.android.wallet.databinding.FragmentAddNoteBinding
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.User
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.ui.component.EmojiIdCopiedViewController
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.send.adapter.GIFThumbnailAdapter
import com.tari.android.wallet.ui.fragment.store.LockBottomSheetBehavior
import com.tari.android.wallet.ui.presentation.TxNote
import com.tari.android.wallet.ui.presentation.gif.GIF
import com.tari.android.wallet.ui.presentation.gif.GIFRepository
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.ui.util.UiUtil.setColor
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.Constants.UI.AddNoteAndSend
import com.tari.android.wallet.util.EmojiUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import io.reactivex.Observer as RxObserver

/**
 * Add a note to the transaction & send it through this fragment.
 *
 * @author The Tari Development Team
 */
class AddNoteFragment : Fragment(), View.OnTouchListener {

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var vmFactory: ThumbnailGIFsViewModelFactory

    private lateinit var listenerWR: WeakReference<Listener>

    // slide button animation related variables
    private var slideButtonXDelta = 0
    private var slideButtonLastMarginStart = 0
    private var slideButtonContainerWidth = 0

    // Formats the summarized emoji id.
    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController

    // Animates the emoji id "copied" text.
    private lateinit var emojiIdCopiedViewController: EmojiIdCopiedViewController

    // Tx properties.
    private lateinit var recipientUser: User
    private lateinit var amount: MicroTari
    private lateinit var fee: MicroTari

    private lateinit var ui: FragmentAddNoteBinding
    private lateinit var viewModel: ThumbnailGIFsViewModel
    private lateinit var gifContainer: GIFContainer
    private lateinit var adapter: GIFThumbnailAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
        listenerWR = WeakReference(context as Listener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FragmentAddNoteBinding.inflate(inflater, container, false).also { ui = it }.root

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            tracker.screen(path = "/home/send_tari/add_note", title = "Send Tari - Add Note")
        }
        initializeGIFsViewModel()
        retrievePageArguments(savedInstanceState)
        setupUI(savedInstanceState)
        setupCTAs()
    }

    private fun initializeGIFsViewModel() {
        viewModel = ViewModelProvider(this, vmFactory)[ThumbnailGIFsViewModel::class.java]
        viewModel.state.observe(viewLifecycleOwner) {
            when {
                it.isSuccessful -> adapter.repopulate(it.gifs!!)
                it.isError -> Logger.e("GIFs request had error")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_GIF) {
            changeScrollViewBottomConstraint(R.id.slide_button_container_view)
            val media =
                data?.getParcelableExtra<Media>(ChooseGIFDialogFragment.MEDIA_DELIVERY_KEY)
                    ?: return
            gifContainer.gif = media.let {
                GIF(it.id, Uri.parse(it.embedUrl), Uri.parse(it.images.original!!.gifUrl))
            }
            updateSliderState()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        gifContainer.save(outState)
    }

    override fun onDestroyView() {
        gifContainer.dispose()
        super.onDestroyView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI(state: Bundle?) {
        gifContainer = GIFContainer(
            Glide.with(this),
            ui.gifContainerView,
            ui.gifImageView,
            ui.searchGiphyContainerView,
            state
        )
        if (gifContainer.gif != null) changeScrollViewBottomConstraint(R.id.slide_button_container_view)
        adapter = GIFThumbnailAdapter(Glide.with(this), ::handleViewMoreGIFsIntent) {
            if (gifContainer.isShown) {
                changeScrollViewBottomConstraint(R.id.slide_button_container_view)
                gifContainer.gif = it
                updateSliderState()
            }
        }
        emojiIdCopiedViewController = EmojiIdCopiedViewController(ui.emojiIdCopiedView)
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView)
        ui.fullEmojiIdBgClickBlockerView.isClickable = false
        ui.fullEmojiIdContainerView.gone()
        displayAliasOrEmojiId()
        hideFullEmojiId(animated = false)
        OverScrollDecoratorHelper.setUpOverScroll(ui.fullEmojiIdScrollView)
        ui.progressBar.setColor(color(white))
        ui.noteEditText.addTextChangedListener(afterTextChanged = { updateSliderState() })
        ui.slideView.setOnTouchListener(this)
        // disable "send" slider
        disableCallToAction()
        focusEditTextAndShowKeyboard()
        ui.promptTextView.setTextColor(color(black))
        ui.noteEditText.imeOptions = EditorInfo.IME_ACTION_DONE
        ui.noteEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        ui.rootView.doOnGlobalLayout {
            UiUtil.setTopMargin(ui.fullEmojiIdContainerView, ui.emojiIdSummaryContainerView.top)
            UiUtil.setHeight(ui.fullEmojiIdContainerView, ui.emojiIdSummaryContainerView.height)
            UiUtil.setWidth(ui.fullEmojiIdContainerView, ui.emojiIdSummaryContainerView.width)
        }
        ui.thumbnailGifsRecyclerView.also {
            val margin = dimen(add_note_gif_inner_margin).toInt()
            it.addItemDecoration(HorizontalInnerMarginDecoration(margin))
            it.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            it.adapter = adapter
        }
    }

    private fun updateSliderState() {
        if (ui.noteEditText.text?.toString().isNullOrEmpty() && gifContainer.gif == null) {
            ui.promptTextView.setTextColor(color(black))
            disableCallToAction()
        } else {
            ui.promptTextView.setTextColor(color(add_note_prompt_passive_color))
            enableCallToAction()
        }
    }

    private fun retrievePageArguments(savedInstanceState: Bundle?) {
        recipientUser = arguments!!.getParcelable("recipientUser")!!
        amount = arguments!!.getParcelable("amount")!!
        fee = arguments!!.getParcelable("fee")!!
        if (savedInstanceState == null) {
            arguments!!.getString(DeepLink.PARAMETER_NOTE)?.let { ui.noteEditText.setText(it) }
        }
    }

    private fun setupCTAs() {
        ui.backButton.setOnClickListener { onBackButtonClicked(it) }
        ui.emojiIdSummaryContainerView.setOnClickListener { emojiIdClicked() }
        ui.dimmerView.setOnClickListener { onEmojiIdDimmerClicked() }
        ui.copyEmojiIdButton.setOnClickListener { onCopyEmojiIdButtonClicked(it) }
        ui.copyEmojiIdButton.setOnLongClickListener { copyEmojiIdButton ->
            onCopyEmojiIdButtonLongClicked(copyEmojiIdButton)
            true
        }
        ui.removeGifCtaView.setOnClickListener {
            changeScrollViewBottomConstraint(R.id.search_giphy_container_view)
            gifContainer.gif = null
            updateSliderState()
        }
        ui.searchGiphyCtaView.setOnClickListener {
            handleViewMoreGIFsIntent()
        }
    }

    private fun handleViewMoreGIFsIntent() {
        if (gifContainer.isShown) {
            UiUtil.hideKeyboard(requireActivity())
            ChooseGIFDialogFragment.newInstance()
                .apply { setTargetFragment(this@AddNoteFragment, REQUEST_CODE_GIF) }
                .show(requireActivity().supportFragmentManager, null)
        }

    }

    private fun changeScrollViewBottomConstraint(toTopOf: Int) {
        val set = ConstraintSet().apply { clone(ui.rootView) }
        set.connect(R.id.message_body_scroll_view, ConstraintSet.BOTTOM, toTopOf, ConstraintSet.TOP)
        set.applyTo(ui.rootView)
    }

    private fun displayAliasOrEmojiId() {
        if (recipientUser is Contact) {
            ui.emojiIdSummaryContainerView.gone()
            ui.titleTextView.visible()
            ui.titleTextView.text = (recipientUser as Contact).alias
        } else {
            displayEmojiId(recipientUser.publicKey.emojiId)
        }
    }

    private fun displayEmojiId(emojiId: String) {
        ui.emojiIdSummaryContainerView.visible()
        emojiIdSummaryController.display(emojiId)
        ui.titleTextView.gone()
        ui.fullEmojiIdTextView.text = EmojiUtil.getFullEmojiIdSpannable(
            emojiId,
            string(emoji_id_chunk_separator),
            color(black),
            color(light_gray)
        )
    }

    private fun onBackButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        // going back before hiding keyboard causes a blank white area on the screen
        // wait a while, then forward the back action to the host activity
        activity?.let {
            UiUtil.hideKeyboard(it)
            ui.rootView.postDelayed(Constants.UI.shortDurationMs, it::onBackPressed)
        }
    }

    /**
     * Display full emoji id and dim out all other views.
     */
    private fun emojiIdClicked() {
        showFullEmojiId()
    }

    private fun showFullEmojiId() {
        ui.fullEmojiIdBgClickBlockerView.isClickable = true
        // make dimmers non-clickable until the anim is over
        ui.dimmerView.isClickable = false
        // prepare views
        ui.emojiIdSummaryContainerView.invisible()
        ui.dimmerView.alpha = 0f
        ui.dimmerView.visible()
        val fullEmojiIdInitialWidth = ui.emojiIdSummaryContainerView.width
        val fullEmojiIdDeltaWidth =
            (ui.rootView.width - dimenPx(common_horizontal_margin) * 2) - fullEmojiIdInitialWidth
        UiUtil.setWidth(
            ui.fullEmojiIdContainerView,
            fullEmojiIdInitialWidth
        )
        ui.fullEmojiIdContainerView.alpha = 0f
        ui.fullEmojiIdContainerView.visible()
        // scroll to end
        ui.fullEmojiIdScrollView.post {
            ui.fullEmojiIdScrollView.scrollTo(
                ui.fullEmojiIdTextView.width - ui.fullEmojiIdScrollView.width,
                0
            )
        }
        ui.copyEmojiIdButtonContainerView.alpha = 0f
        ui.copyEmojiIdButtonContainerView.visible()
        UiUtil.setBottomMargin(
            ui.copyEmojiIdButtonContainerView,
            0
        )
        // animate full emoji id view
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.dimmerView.alpha = value * 0.6f
            // container alpha & scale
            ui.fullEmojiIdContainerView.alpha = value
            ui.fullEmojiIdContainerView.scaleX = 1f + 0.2f * (1f - value)
            ui.fullEmojiIdContainerView.scaleY = 1f + 0.2f * (1f - value)
            UiUtil.setWidth(
                ui.fullEmojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
            ui.backButton.alpha = 1 - value
        }
        emojiIdAnim.duration = Constants.UI.shortDurationMs
        // copy emoji id button anim
        val copyEmojiIdButtonAnim = ValueAnimator.ofFloat(0f, 1f)
        copyEmojiIdButtonAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.copyEmojiIdButtonContainerView.alpha = value
            UiUtil.setBottomMargin(
                ui.copyEmojiIdButtonContainerView,
                (dimenPx(common_copy_emoji_id_button_visible_bottom_margin) * value).toInt()
            )
        }
        copyEmojiIdButtonAnim.duration = Constants.UI.shortDurationMs
        copyEmojiIdButtonAnim.interpolator = EasingInterpolator(Ease.BACK_OUT)

        // chain anim.s and start
        val animSet = AnimatorSet()
        animSet.playSequentially(emojiIdAnim, copyEmojiIdButtonAnim)
        animSet.start()
        animSet.addListener(onEnd = { ui.dimmerView.isClickable = true })
        // scroll animation
        ui.fullEmojiIdScrollView.postDelayed(Constants.UI.shortDurationMs + 20) {
            ui.fullEmojiIdScrollView.smoothScrollTo(0, 0)
        }
    }

    private fun hideFullEmojiId(animateCopyEmojiIdButton: Boolean = true, animated: Boolean) {
        if (!animated) {
            ui.fullEmojiIdContainerView.gone()
            ui.dimmerView.gone()
            ui.copyEmojiIdButtonContainerView.gone()
            return
        }
        ui.fullEmojiIdScrollView.smoothScrollTo(0, 0)
        ui.emojiIdSummaryContainerView.visible()
        ui.emojiIdSummaryContainerView.alpha = 0f
        // copy emoji id button anim
        val copyEmojiIdButtonAnim = ValueAnimator.ofFloat(1f, 0f)
        copyEmojiIdButtonAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.copyEmojiIdButtonContainerView.alpha = value
            UiUtil.setBottomMargin(
                ui.copyEmojiIdButtonContainerView,
                (dimenPx(common_copy_emoji_id_button_visible_bottom_margin) * value).toInt()
            )
        }
        copyEmojiIdButtonAnim.duration = Constants.UI.shortDurationMs
        // emoji id anim
        val fullEmojiIdInitialWidth = ui.fullEmojiIdContainerView.width
        val fullEmojiIdDeltaWidth =
            ui.emojiIdSummaryContainerView.width - ui.fullEmojiIdContainerView.width
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.dimmerView.alpha = (1 - value) * 0.6f
            // container alpha & scale
            ui.fullEmojiIdContainerView.alpha = (1 - value)
            UiUtil.setWidth(
                ui.fullEmojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
            ui.emojiIdSummaryContainerView.alpha = value
            ui.backButton.alpha = value
        }
        emojiIdAnim.duration = Constants.UI.shortDurationMs
        // chain anim.s and start
        val animSet = AnimatorSet()
        if (animateCopyEmojiIdButton) {
            animSet.playSequentially(copyEmojiIdButtonAnim, emojiIdAnim)
        } else {
            animSet.play(emojiIdAnim)
        }
        animSet.start()
        animSet.addListener(onEnd = {
            ui.dimmerView.gone()
            ui.fullEmojiIdBgClickBlockerView.isClickable = false
            ui.fullEmojiIdContainerView.gone()
            ui.copyEmojiIdButtonContainerView.gone()
        })
    }

    /**
     * Dimmer clicked - hide dimmers.
     */
    private fun onEmojiIdDimmerClicked() {
        hideFullEmojiId(animated = true)
    }

    private fun focusEditTextAndShowKeyboard() {
        val mActivity = activity ?: return
        ui.noteEditText.requestFocus()
        val imm = mActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(
            InputMethodManager.SHOW_FORCED,
            InputMethodManager.HIDE_IMPLICIT_ONLY
        )
    }

    private fun completeCopyEmojiId(clipboardString: String) {
        ui.dimmerView.isClickable = false
        val mActivity = activity ?: return
        val clipBoard = ContextCompat.getSystemService(mActivity, ClipboardManager::class.java)
        val clipboardData = ClipData.newPlainText(
            "Tari Wallet Identity",
            clipboardString
        )
        clipBoard?.setPrimaryClip(clipboardData)
        emojiIdCopiedViewController.showEmojiIdCopiedAnim(fadeOutOnEnd = true) {
            hideFullEmojiId(animateCopyEmojiIdButton = false, animated = true)
        }
        // hide copy emoji id button
        val copyEmojiIdButtonAnim = ui.copyEmojiIdButtonContainerView.animate().alpha(0f)
        copyEmojiIdButtonAnim.duration = Constants.UI.xShortDurationMs
        copyEmojiIdButtonAnim.start()
    }

    private fun onCopyEmojiIdButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        completeCopyEmojiId(recipientUser.publicKey.emojiId)
    }

    private fun onCopyEmojiIdButtonLongClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        completeCopyEmojiId(recipientUser.publicKey.hexString)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableCallToAction() {
        if (ui.slideEnabledBgView.visibility == View.VISIBLE) {
            return
        }
        ui.slideView.setOnTouchListener(this)

        ui.slideToSendDisabledTextView.invisible()

        ui.slideToSendEnabledTextView.alpha = 0f
        ui.slideToSendEnabledTextView.visible()
        ui.slideToSendArrowEnabledImageView.alpha = 0f
        ui.slideToSendArrowEnabledImageView.visible()
        ui.slideEnabledBgView.alpha = 0f
        ui.slideEnabledBgView.visible()

        val textViewAnim = ObjectAnimator.ofFloat(ui.slideToSendEnabledTextView, "alpha", 0f, 1f)
        val arrowAnim = ObjectAnimator.ofFloat(ui.slideToSendArrowEnabledImageView, "alpha", 0f, 1f)
        val bgViewAnim = ObjectAnimator.ofFloat(ui.slideEnabledBgView, "alpha", 0f, 1f)

        // the animation set
        val animSet = AnimatorSet()
        animSet.playTogether(textViewAnim, arrowAnim, bgViewAnim)
        animSet.duration = Constants.UI.shortDurationMs
        animSet.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun disableCallToAction() {
        ui.slideToSendDisabledTextView.visible()
        ui.slideEnabledBgView.gone()
        ui.slideToSendEnabledTextView.gone()
        ui.slideToSendArrowEnabledImageView.gone()
        ui.slideView.setOnTouchListener(null)
    }

    /**
     * Controls the slide button animation & behaviour on drag.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(
        view: View,
        event: MotionEvent
    ): Boolean {
        val x = event.rawX.toInt()
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                slideButtonContainerWidth = ui.slideButtonContainerView.width
                val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
                slideButtonXDelta = x - layoutParams.marginStart
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> {
            }
            MotionEvent.ACTION_MOVE -> {
                val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
                val newLeftMargin = x - slideButtonXDelta
                slideButtonLastMarginStart = if (newLeftMargin < dimenPx(
                        add_note_slide_button_left_margin
                    )
                ) {
                    dimenPx(add_note_slide_button_left_margin)
                } else {
                    if (newLeftMargin + dimenPx(add_note_slide_button_width) + dimenPx(
                            add_note_slide_button_left_margin
                        ) >= slideButtonContainerWidth
                    ) {
                        slideButtonContainerWidth - dimenPx(add_note_slide_button_width) - dimenPx(
                            add_note_slide_button_left_margin
                        )
                    } else {
                        x - slideButtonXDelta
                    }
                }
                layoutParams.marginStart = slideButtonLastMarginStart
                val alpha =
                    1f - slideButtonLastMarginStart.toFloat() /
                            (slideButtonContainerWidth -
                                    dimenPx(add_note_slide_button_left_margin) -
                                    dimenPx(add_note_slide_button_width))
                ui.slideToSendEnabledTextView.alpha = alpha
                ui.slideToSendDisabledTextView.alpha = alpha

                view.layoutParams = layoutParams
            }
            MotionEvent.ACTION_UP -> if (slideButtonLastMarginStart < slideButtonContainerWidth / 2) {
                val anim = ValueAnimator.ofInt(
                    slideButtonLastMarginStart,
                    dimenPx(add_note_slide_button_left_margin)
                )
                anim.addUpdateListener { valueAnimator: ValueAnimator ->
                    val margin = valueAnimator.animatedValue as Int
                    UiUtil.setStartMargin(ui.slideView, margin)
                    ui.slideToSendEnabledTextView.alpha =
                        1f - margin.toFloat() / (slideButtonContainerWidth - dimenPx(
                            add_note_slide_button_left_margin
                        ) - dimenPx(add_note_slide_button_width))
                }
                anim.duration = Constants.UI.shortDurationMs
                anim.interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
                anim.startDelay = 0
                anim.start()
            } else {
                // disable input
                ui.noteEditText.inputType = InputType.TYPE_NULL
                // complete slide animation
                val anim = ValueAnimator.ofInt(
                    slideButtonLastMarginStart,
                    slideButtonContainerWidth - dimenPx(add_note_slide_button_left_margin) - dimenPx(
                        add_note_slide_button_width
                    )
                )
                anim.addUpdateListener { valueAnimator: ValueAnimator ->
                    val margin = valueAnimator.animatedValue as Int
                    UiUtil.setStartMargin(ui.slideView, margin)
                    ui.slideToSendEnabledTextView.alpha =
                        1f - margin.toFloat() / (slideButtonContainerWidth -
                                dimenPx(add_note_slide_button_left_margin) -
                                dimenPx(add_note_slide_button_width))
                }
                anim.duration = Constants.UI.shortDurationMs
                anim.interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
                anim.startDelay = 0
                anim.addListener(onEnd = { slideAnimationCompleted() })
                anim.start()
            }
        }
        return false
    }

    private fun slideAnimationCompleted() {
        // hide slide view
        val anim = ValueAnimator.ofFloat(1F, 0F)
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            ui.slideView.alpha = valueAnimator.animatedValue as Float
        }
        anim.duration = Constants.UI.shortDurationMs
        anim.interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
        anim.startDelay = 0
        anim.addListener(onEnd = {
            onSlideAnimationEnd()
            anim.removeAllListeners()
        })
        anim.start()
    }

    private fun onSlideAnimationEnd() {
        if (EventBus.networkConnectionStateSubject.value != NetworkConnectionState.CONNECTED) {
            ui.rootView.postDelayed(AddNoteAndSend.preKeyboardHideWaitMs) { hideKeyboard() }
            ui.rootView.postDelayed(AddNoteAndSend.preKeyboardHideWaitMs + Constants.UI.keyboardHideWaitMs) {
                restoreSlider()
                ui.noteEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
                showInternetConnectionErrorDialog(activity!!)
            }
        } else {
            ui.removeGifCtaView.isEnabled = false
            ui.progressBar.visible()
            ui.slideView.gone()
            ui.rootView.postDelayed(AddNoteAndSend.preKeyboardHideWaitMs) { hideKeyboard() }
            val totalTime =
                AddNoteAndSend.preKeyboardHideWaitMs + AddNoteAndSend.continueToFinalizeSendTxDelayMs
            ui.rootView.postDelayed(totalTime) { continueToFinalizeSendTx() }
        }
    }

    private fun hideKeyboard() {
        UiUtil.hideKeyboard(activity ?: return)
        ui.noteEditText.clearFocus()
    }

    private fun continueToFinalizeSendTx() {
        // track event
        tracker.event(category = "Transaction", action = "Transaction Initiated")
        // notify listener (i.e. activity)
        listenerWR.get()?.continueToFinalizeSendTx(
            this,
            recipientUser,
            amount,
            fee,
            TxNote(
                ui.noteEditText.editableText.toString(),
                gifContainer.gif?.embedUri?.toString()
            ).compose()
        )
    }

    private fun restoreSlider() {
        // hide slide view
        val slideViewInitialMargin = UiUtil.getStartMargin(ui.slideView)
        val slideViewMarginDelta =
            dimenPx(add_note_slide_button_left_margin) - slideViewInitialMargin
        val anim = ValueAnimator.ofFloat(
            1f,
            0f
        )
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.slideView.alpha = 1f - value
            ui.slideToSendEnabledTextView.alpha = 1f - value
            UiUtil.setStartMargin(
                ui.slideView,
                (slideViewInitialMargin + slideViewMarginDelta * (1 - value)).toInt()
            )
        }
        anim.duration = Constants.UI.shortDurationMs
        anim.interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
        anim.start()
    }

    /**
     * Listener interface - to be implemented by the host activity.
     */
    interface Listener {

        fun continueToFinalizeSendTx(
            sourceFragment: AddNoteFragment,
            recipientUser: User,
            amount: MicroTari,
            fee: MicroTari,
            note: String
        )

    }

    private inner class GIFContainer(
        private val glide: RequestManager,
        private val gifContainerView: View,
        private val gifView: ImageView,
        thumbnailsContainer: View,
        state: Bundle?
    ) {

        private val transformation = RequestOptions().transform(RoundedCorners(10))
        private var animation = GIFsPanelAnimation(thumbnailsContainer)

        val isShown: Boolean
            get() = animation.isViewShown

        var gif: GIF? = null
            set(value) {
                field = value
                if (value == null) {
                    glide.clear(gifView)
                    showContainer()
                } else {
                    glide.asGif()
                        .placeholder(R.drawable.background_gif_loading)
                        .apply(transformation)
                        .load(value.uri)
                        .into(gifView)
                    showGIF()
                }
            }

        init {
            gif = state?.getParcelable(KEY_GIF)
        }

        private fun showContainer() {
            animation.show()
            gifContainerView.gone()
        }

        private fun showGIF() {
            animation.hide()
            gifContainerView.visible()
        }

        fun save(bundle: Bundle) {
            gif?.run { bundle.putParcelable(KEY_GIF, this) }
        }

        fun dispose() {
            animation.dispose()
        }

    }

    private data class GIFsPanelAnimationState(
        val direction: TranslationDirection,
        val animator: Animator?
    )

    private enum class TranslationDirection { UP, DOWN }

    private class GIFsPanelAnimation(private val view: View) {
        private var state =
            GIFsPanelAnimationState(TranslationDirection.UP, null)
        val isViewShown
            get() = state.direction == TranslationDirection.UP

        fun show() {
            state.animator?.cancel()
            state = createState(TranslationDirection.UP, to = 0F)
        }

        fun hide() {
            state.animator?.cancel()
            state = createState(TranslationDirection.DOWN, to = view.height.toFloat())
        }

        private fun createState(direction: TranslationDirection, to: Float) =
            GIFsPanelAnimationState(
                direction,
                ValueAnimator.ofFloat(view.translationY, to).apply {
                    duration = TRANSLATION_DURATION
                    addUpdateListener {
                        view.translationY = it.animatedValue as Float
                    }
                    start()
                })

        fun dispose() {
            this.state.animator?.cancel()
        }

        private companion object {
            private const val TRANSLATION_DURATION = 300L
        }
    }

    class ThumbnailGIFsViewModelFactory(private val repository: GIFRepository) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            require(modelClass === ThumbnailGIFsViewModel::class.java)
            return ThumbnailGIFsViewModel(repository) as T
        }
    }

    private class ThumbnailGIFsViewModel(private val gifsRepository: GIFRepository) : ViewModel() {

        private val _state = MutableLiveData<GIFsState>()
        val state: LiveData<GIFsState> get() = _state

        init {
            fetchGIFs()
        }

        private fun fetchGIFs() {
            viewModelScope.launch(Dispatchers.Main) {
                _state.value = GIFsState()
                try {
                    val gifs = withContext(Dispatchers.IO) {
                        gifsRepository.getAll(THUMBNAIL_REQUEST_QUERY, THUMBNAIL_REQUEST_LIMIT)
                    }
                    _state.value = GIFsState(gifs)
                } catch (e: Exception) {
                    Logger.e(e, "Error occurred while fetching gifs")
                    _state.value = GIFsState(e)
                }
            }
        }

        class GIFsState private constructor(val gifs: List<GIF>?, val error: Exception?) {
            // Loading state
            constructor() : this(null, null)
            constructor(gifs: List<GIF>) : this(gifs, null)
            constructor(e: Exception) : this(null, e)

            val isError get() = error != null
            val isSuccessful get() = gifs != null
        }
    }

    private class HorizontalInnerMarginDecoration(private val value: Int) :
        RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            if (parent.getChildLayoutPosition(view) > 0) outRect.left = value
        }
    }

    class ChooseGIFDialogFragment @Deprecated(
        """Use newInstance() and supply all the necessary data via arguments instead, as fragment's 
default no-op constructor is used by the framework for UI tree rebuild on configuration changes"""
    ) constructor() : DialogFragment() {
        private lateinit var ui: DialogChooseGifBinding
        private lateinit var behavior: LockBottomSheetBehavior<View>
        private lateinit var searchSubscription: Disposable

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View = DialogChooseGifBinding.inflate(inflater, container, false).also { ui = it }.root

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val searchSubject = BehaviorSubject.create<String>()
            searchSubscription = searchSubject
                .debounce(500L, TimeUnit.MILLISECONDS)
                .map { if (it.isEmpty()) INITIAL_REQUEST_QUERY else it }
                .map { GPHContent.searchQuery(it, mediaType = MediaType.gif) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { ui.giphyGridView.content = it }
            setupUI(searchSubject)
        }

        override fun onStop() {
            super.onStop()
            dialog!!.window!!.setWindowAnimations(R.style.ChooseGIFBottomNoDialogAnimation)
        }

        override fun onDestroyView() {
            searchSubscription.dispose()
            super.onDestroyView()
        }

        private fun setupUI(observer: RxObserver<String>) {
            ui.giphyGridView.content = INITIAL_REQUEST
            ui.giphyGridView.callback = object : GPHGridCallback {
                override fun contentDidUpdate(resultCount: Int) {
                    // No-op
                }

                override fun didSelectMedia(media: Media) {
                    val intent = Intent().apply { putExtra(MEDIA_DELIVERY_KEY, media) }
                    targetFragment!!.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
                    behavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
            ui.gifSearchEditText.addTextChangedListener(
                afterTextChanged = afterChanged@{
                    observer.onNext(it?.toString() ?: return@afterChanged)
                }
            )
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            BottomSheetDialog(requireContext(), R.style.ChooseGIFDialog)
                .apply { setOnShowListener { setupDialog(this) } }

        private fun setupDialog(bottomSheetDialog: BottomSheetDialog) {
            val bottomSheet: View = bottomSheetDialog.findViewById(R.id.design_bottom_sheet)!!
            behavior = LockBottomSheetBehavior()
            behavior.isHideable = true
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            val rootView = bottomSheet.parent as View
            rootView.setBackgroundColor(Color.BLACK)
            behavior.addBottomSheetCallback(DismissOnHide(this, rootView))
            val layoutParams = bottomSheet.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.behavior = behavior
            bottomSheetDialog.setOnKeyListener { _, keyCode, _ ->
                (keyCode == KeyEvent.KEYCODE_BACK && behavior.state != BottomSheetBehavior.STATE_HIDDEN &&
                        behavior.state != BottomSheetBehavior.STATE_COLLAPSED).also {
                    if (it) behavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }

        private class DismissOnHide(
            private val fragment: DialogFragment,
            private val rootView: View
        ) :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    fragment.dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val color: Int = Color.argb((slideOffset.coerceIn(0F, 1F) * 255).toInt(), 0, 0, 0)
                rootView.setBackgroundColor(color)
            }
        }

        companion object {
            @Suppress("DEPRECATION")
            fun newInstance() = ChooseGIFDialogFragment()
            const val MEDIA_DELIVERY_KEY = "key_media"
            private const val INITIAL_REQUEST_QUERY = "money"
            private val INITIAL_REQUEST =
                GPHContent.searchQuery(INITIAL_REQUEST_QUERY, mediaType = MediaType.gif)
        }

    }


    private companion object {
        private const val KEY_GIF = "keygif"
        private const val REQUEST_CODE_GIF = 1535
        private const val THUMBNAIL_REQUEST_QUERY = "money"
        private const val THUMBNAIL_REQUEST_LIMIT = 20
    }

}
