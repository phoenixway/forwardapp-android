package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.shared.core.models.link.CanonicalLinkType
import com.romankozak.forwardappmobile.shared.core.models.link.CanonicalRelatedLink

internal fun RelatedLink.toCanonicalRelatedLink(): CanonicalRelatedLink =
    CanonicalRelatedLink(
        type = type?.let { CanonicalLinkType.valueOf(it.name) },
        target = target,
        displayName = displayName,
        vault = vault,
    )

internal fun CanonicalRelatedLink.toAndroidRelatedLink(): RelatedLink =
    RelatedLink(
        type = type?.let { LinkType.valueOf(it.name) },
        target = target,
        displayName = displayName,
        vault = vault,
    )

internal fun List<RelatedLink>?.toCanonicalRelatedLinks(): List<CanonicalRelatedLink> =
    orEmpty().map { link -> link.toCanonicalRelatedLink() }

internal fun List<CanonicalRelatedLink>.toAndroidRelatedLinks(): List<RelatedLink> =
    map { link -> link.toAndroidRelatedLink() }
