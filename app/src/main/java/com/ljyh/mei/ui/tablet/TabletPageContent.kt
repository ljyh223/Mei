package com.ljyh.mei.ui.tablet

import com.ljyh.mei.ui.app.MeiPageContent
import com.ljyh.mei.ui.app.SharedMeiPageContent

/** Shared pages adapt to their own viewport; future distinct tablet pages can be selected here. */
internal object TabletPageContent : MeiPageContent by SharedMeiPageContent
