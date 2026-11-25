// Demo community posts (MongoDB). Run after seed-postgres.sql: the authors, photos and rescue
// cases are the ones it creates.
//
//   mongosh "$STAGING_MONGO_URI" scripts/demo-data/seed-mongo.js
//
// Safe to run again; existing posts are left alone. To remove the posts:
//
//   mongosh "$STAGING_MONGO_URI" --eval "var CLEANUP = true" scripts/demo-data/seed-mongo.js
//
// Writes to the database the API reads (MONGO_DATABASE, default "petrescue"), not the one in the
// connection string: an Atlas URI usually names none, and mongosh would then write to "test".
// For another name: --eval "var DB_NAME = 'name'". The app must have started once against that
// database, so collections and indexes exist; the script stops if "posts" is missing.

const { createHash } = require('crypto');

const target = db.getSiblingDB(typeof DB_NAME !== 'undefined' && DB_NAME ? DB_NAME : 'petrescue');
if (!target.getCollectionNames().includes('posts')) {
    throw new Error('Database "' + target.getName() + '" has no "posts" collection. Start the API against it once, '
        + 'or pass the right name with --eval "var DB_NAME = \'name\'".');
}

// Same derivation as pg_temp.did in seed-postgres.sql.
function did(kind, n) {
    const h = createHash('md5').update('pet-rescue-demo:' + kind + ':' + n).digest('hex');
    return UUID(h.slice(0, 8) + '-' + h.slice(8, 12) + '-' + h.slice(12, 16) + '-' + h.slice(16, 20) + '-' + h.slice(20));
}

// The same lists, in the same order, as seed-postgres.sql.
const USER_NAMES = ['Nguyễn Minh Anh', 'Trần Gia Bảo', 'Lê Thu Hà', 'Phạm Quốc Huy', 'Hoàng Ngọc Lan', 'Vũ Đức Thịnh',
    'Đặng Thảo Vy', 'Bùi Tuấn Kiệt', 'Đỗ Khánh Linh', 'Ngô Hải Đăng', 'Dương Mỹ Duyên', 'Lý Hoàng Nam',
    'Phan Bảo Ngọc', 'Võ Thanh Tùng', 'Trịnh Diệu Linh', 'Mai Anh Khoa', 'Hồ Thanh Trúc', 'Lâm Gia Huy',
    'Tạ Minh Châu', 'Châu Ngọc Hân', 'Nguyễn Đức Long', 'Trần Phương Thảo', 'Lê Hoàng Phúc', 'Phạm Thu Trang',
    'Huỳnh Tấn Phát', 'Đinh Ngọc Ánh', 'Cao Minh Quân', 'Vương Bích Ngọc', 'Kiều Anh Tú', 'Lưu Quỳnh Như'];
const PHOTOS = {
    'pet-dog': ['1552053831-71594a27632d', '1601979031925-424e53b6caaa', '1561037404-61cd46aa615b', '1534361960057-19889db9621e',
        '1503256207526-0d5d80fa2f47', '1537151608828-ea2b11777ee8', '1600804340584-c7db2eacf0bf', '1518020382113-a7e8fc38eac9',
        '1537151625747-768eb6cf92b2', '1560807707-8cc77767d783', '1591160690555-5debfba289f0', '1504826260979-242151ee45b7',
        '1760615302825-900295f63a2c', '1629555256545-e29db1b05f86', '1507543139012-85b5900144e4', '1700893945560-f1e4f3513e33',
        '1636772611113-d44a76913388', '1552036354-d0ee14b417e0'],
    'pet-cat': ['1592194996308-7b43878e84a6', '1573865526739-10659fec78a5', '1574144611937-0df059b5ef3e', '1561948955-570b270e7c36',
        '1560114928-40f1f1eb26a0', '1472491235688-bdc81a63246e', '1529778873920-4da4926a72c2', '1478098711619-5ab0b478d6e6',
        '1595433707802-6b2626ef1c91', '1533743983669-94fa5c4338ec', '1555685812-4b943f1cb0eb', '1494256997604-768d1f608cac',
        '1492370284958-c20b15c692d2', '1757430757969-3d2b92025097', '1763839361290-4d711e42ab5f', '1530991671072-ac4f81c2c3c1'],
    'case-dog': ['1770185584605-95e864143593', '1573828260583-ec780bbe0c1a', '1785472895235-41a813f52160', '1629555256341-09e5e9871647',
        '1759289615419-509c6da47a23', '1558623535-33cc88178982', '1764306737125-8f632ef29ea1', '1709497083548-b6669f390b64',
        '1444212477490-ca407925329e', '1553434133-96822a8e94af', '1516280287949-2747a3304a2f'],
    'case-cat': ['1604675223954-b1aabd668078', '1760618813526-66e940f1d84b', '1503362516536-635096dd5a80', '1609779361684-8196b3a0abf1',
        '1761731828949-a78d41e0057a', '1622273413883-265d478feda3', '1498579687545-d5a4fffb0a9e', '1759239432958-34a6ae1062fc',
        '1765044312946-d4ebfb674608', '1637199809036-cfda31b63399', '1490481920145-fc78891bbb99', '1650036786401-30f469106781'],
};
// City of demo_place idx 0..23.
const CITIES = ['HCM', 'HN', 'DN', 'HCM', 'HCM', 'HN', 'DN', 'HCM', 'HCM', 'HCM', 'HCM', 'HCM', 'HCM', 'HCM', 'HCM',
    'HN', 'HN', 'HN', 'HN', 'HN', 'HN', 'DN', 'DN', 'DN']
    // the address catalogue's own names, which the post province filter matches
    .map((c) => ({ HCM: 'Thành phố Hồ Chí Minh', HN: 'Thành phố Hà Nội', DN: 'Thành phố Đà Nẵng' }[c]));

function media(kind, idx) {
    return {
        mediaId: did('photo-' + kind, idx),
        publicId: 'https://images.unsplash.com/photo-' + PHOTOS[kind][idx] + '?w=1200&q=80&auto=format&fit=crop',
    };
}

// Rescue case n of seed-postgres.sql: species, photo, city and status follow the same rules.
function rescueCase(n) {
    const dog = n % 2 === 1;
    const kind = dog ? 'case-dog' : 'case-cat';
    const r = n % 20;
    return {
        caseId: did('case', n),
        status: r < 7 ? 'REPORTED' : r < 12 ? 'IN_PROGRESS' : r < 15 ? 'RESCUED' : 'CLOSED',
        species: dog ? 'Dog' : 'Cat',
        media: media(kind, (Math.floor((n + 1) / 2) - 1) % PHOTOS[kind].length),
        province: CITIES[n % 24],
    };
}

// Tags are slug codes from the tags table; facets use catalog values (species in English).
const POSTS = [
    { caseN: 1, tags: ['rescue'], text: 'Bé cún vện này đi cà nhắc quanh hẻm nhà mình từ sáng, chân sau bị thương. Mình đã báo ca trên bản đồ, ai ở gần ghé giúp một tay với ạ!' },
    { caseN: 2, tags: ['rescue'], text: 'Có bé mèo kẹt dưới gầm xe ở bãi giữ xe, kêu suốt từ tối qua. Mình không với tới được, cần người có kinh nghiệm bắt mèo hỗ trợ.' },
    { pet: ['pet-cat', 2], category: 'ADOPTION', species: 'Cat', tags: ['adoption'], text: 'Miu đã khỏe hẳn sau hai tuần điều trị ở trạm, giờ ăn khỏe và rất thích được ôm. Bé đang chờ một gia đình mới, mọi người xem hồ sơ ở mục Nhận nuôi nhé.' },
    { caseN: 3, tags: ['rescue'], text: 'Bé cún nâu này nằm bẹp một chỗ trong nhà xe từ hôm qua, cho ăn không chịu ăn. Mình lo bé bị bệnh, cần người có kinh nghiệm đến xem giúp.' },
    { pet: ['pet-dog', 6], category: 'ADOPTION', species: 'Dog', tags: ['adoption'], text: 'Gạo là bé Golden được cứu từ một trại nhân giống bỏ hoang. Bé đã tiêm đủ mũi, cực kỳ thân thiện với trẻ con. Ai có sân vườn rộng thì hợp với bé lắm.' },
    { pet: ['pet-dog', 15], category: 'LOST', species: 'Dog', tags: ['lost-pet'], text: 'TÌM CHÓ LẠC: bé cún nâu trắng, đeo vòng cổ đỏ, lạc gần chợ từ chiều thứ Bảy. Bé tên Đậu, rất nhát. Ai thấy xin nhắn giúp gia đình mình, xin hậu tạ.' },
    { caseN: 4, tags: ['rescue'], text: 'Bé mèo cam trắng gầy trơ xương ở góc chợ, mắt có ghèn. Mình cho ăn rồi nhưng bé cần được đưa đi khám. Có trạm nào nhận giúp không ạ?' },
    { pet: ['pet-cat', 14], category: 'GENERAL', species: 'Cat', tags: ['care'], text: 'Chia sẻ kinh nghiệm cho bạn nào lần đầu nuôi mèo con: hai tuần đầu nên để bé ở một phòng nhỏ, có khay cát và chỗ trốn. Đừng ép bé ra chơi, bé sẽ tự ra khi sẵn sàng.' },
    { caseN: 5, tags: ['rescue'], text: 'Chú chó trắng đen này nằm giữa đường ở bãi xe tải mấy hôm nay, rất nguy hiểm. Bé hiền nhưng sợ người lạ, cần người đến đưa bé về nơi an toàn.' },
    { pet: ['pet-dog', 3], category: 'GENERAL', species: 'Dog', tags: ['volunteer'], text: 'Trạm cần 4 tình nguyện viên phụ tắm và dắt các bé đi dạo sáng Chủ nhật này. Không cần kinh nghiệm, chỉ cần yêu chó mèo và chịu được lông dính đầy áo!' },
    { caseN: 6, tags: ['rescue'], text: 'Bé mèo đen bị dính bẫy keo chuột, lông bết hết lại. Mình đã bế bé về nhưng không biết xử lý keo thế nào, nhờ mọi người chỉ giúp hoặc nhận ca.' },
    { pet: ['pet-cat', 7], category: 'FOUND', species: 'Cat', tags: ['found-pet'], text: 'Nhặt được bé mèo mướp nâu đi lạc vào sân nhà, bé sạch sẽ và rất dạn người nên chắc là mèo nhà. Chủ của bé liên hệ mình nhé, mình giữ bé tới cuối tuần.' },
    { caseN: 13, tags: ['rescue'], text: 'Cập nhật: bé cún bị kẹt ở cống hôm trước đã được các anh chị trạm cứu hộ đưa lên an toàn. Bé chỉ trầy nhẹ, đang được theo dõi ở trạm. Cảm ơn mọi người rất nhiều!' },
    { pet: ['pet-dog', 0], category: 'ADOPTION', species: 'Dog', tags: ['adoption'], text: 'Một tháng sau khi về nhà mới, Lu đã tăng 3 ký và có hẳn một cái ghế sofa riêng. Cảm ơn trạm đã tin tưởng giao bé cho gia đình mình.' },
    { pet: ['pet-cat', 4], category: 'GENERAL', species: 'Cat', tags: ['care'], text: 'Nhắc nhẹ mùa nắng nóng: luôn để nước sạch cho các bé, đừng cạo trọc lông mèo, và tránh dắt chó đi dạo lúc mặt đường còn nóng nhé cả nhà.' },
    { caseN: 21, tags: ['rescue'], text: 'Bé cún con này nằm co ro ở bậc cầu thang chung cư từ hôm qua, không thấy mẹ đâu. Mình đã cho bé uống sữa, cần người nhận ca đưa bé về trạm.' },
    { pet: ['pet-dog', 10], category: 'GENERAL', species: 'Dog', tags: ['donation'], text: 'Trạm vừa đón thêm 6 bé trong tuần này nên đang thiếu hạt và tã lót. Bạn nào có đồ dư gửi giúp trạm với ạ, đồ cũ còn dùng được cũng quý lắm.' },
    { pet: ['pet-cat', 11], category: 'ADOPTION', species: 'Cat', tags: ['adoption'], text: 'Muối là bé tai cụp bị bỏ ở phòng khám vì chủ không trả viện phí. Bé nhát lúc đầu nhưng quen rồi thì dính người cả ngày. Ưu tiên nhà không nuôi chó lớn.' },
    { caseN: 22, tags: ['rescue'], text: 'Mèo mẹ đẻ con trên mái tôn nhà bỏ hoang, trời thì mưa suốt. Cần người có thang và lồng để đưa cả mẹ lẫn con xuống.' },
    { pet: ['pet-dog', 7], category: 'GENERAL', species: 'Dog', tags: ['care'], text: 'Bạn nào mới nhận nuôi chó ta nhớ tiêm đủ vaccine dại và 7 bệnh nha. Trạm có danh sách phòng khám giảm giá cho thú được cứu hộ, nhắn trạm để lấy.' },
    { caseN: 14, tags: ['rescue'], text: 'Tin vui: bé mèo bị bẫy keo đã được làm sạch bằng dầu ăn và tắm lại, giờ lông mượt như chưa có chuyện gì. Bé đang ở nhà tạm chờ tìm chủ.' },
    { pet: ['pet-cat', 0], category: 'LOST', species: 'Cat', tags: ['lost-pet'], text: 'Mèo nhà mình tên Mochi, lông trắng nâu dài, lạc từ tối thứ Năm quanh khu chung cư. Bé đã triệt sản, có đeo vòng chuông. Ai thấy báo giúp mình với.' },
    { pet: ['pet-dog', 12], category: 'GENERAL', species: 'Dog', tags: ['volunteer'], text: 'Cuối tháng này trạm tổ chức ngày hội nhận nuôi ở công viên. Cần các bạn hỗ trợ dựng gian, chụp ảnh và chăm các bé trong ngày. Đăng ký ở phần bình luận nhé.' },
    { caseN: 23, tags: ['rescue'], text: 'Bé cún nâu này bị ghẻ khá nặng, hay nằm ở bậc thềm đá gần nhà mình. Bé vẫn ăn uống được, cần người đưa đi điều trị da.' },
];

// ── demo.user1's own posts: what the AI read, one waiting for user1 to confirm, one confirmed ──
const hoursAgo = (h) => new Date(Date.now() - h * 3600 * 1000);
const USER1 = { userId: did('user', 1), fullName: USER_NAMES[0] };
const OWN_POSTS = [
    {
        n: 9001, status: 'ANALYZED', hours: 3, photo: ['pet-cat', 5], tags: ['lost-pet'],
        text: 'mn oi meo nha minh ten Muc, long den tuyen, deo vong co do, lac o gan cho Ba Chieu q.binh thanh tu toi hom kia. ai thay ib minh voi, cam on nhieu!!',
        suggested: {
            category: 'LOST', species: 'Cat', locationText: 'Gần chợ Bà Chiểu', district: 'Phường Bình Thạnh',
            province: 'Thành phố Hồ Chí Minh', needsRescue: false, rescueSummary: null, urgency: null,
            tags: ['lost-pet'], categoryConfidence: 0.93, speciesConfidence: 0.97, locationConfidence: 0.82,
        },
        facets: { category: 'GENERAL', species: null, district: null, province: null, needsRescue: false },
    },
    {
        n: 9002, status: 'CONFIRMED', hours: 26, photo: ['pet-dog', 9], tags: ['care'],
        text: 'Gấu nhà mình hồi phục gần hết rồi nè, giờ chạy nhảy cả ngày. Cảm ơn phòng khám An Tâm đã giảm phí cho bé được cứu hộ.',
        suggested: {
            category: 'GENERAL', species: 'Dog', locationText: null, district: null, province: null, needsRescue: false,
            rescueSummary: null, urgency: null, tags: ['care'], categoryConfidence: 0.88, speciesConfidence: 0.95,
            locationConfidence: 0.1,
        },
        facets: { category: 'GENERAL', species: 'Dog', district: null, province: null, needsRescue: false },
    },
];

// ── demo.user1's chats: a friend, the centre about the application, the rescuer of case R-D0030 ──
// Three people, each pair once: [peer kind, peer n, peer name, type, related row, conversation name, related info, messages]
// Each message is [from user1?, text, hours ago]. The last message from the peer stays unread for user1.
const CHATS = [
    { n: 1, peer: ['user', 2, USER_NAMES[1]], type: 'GENERAL', related: null, name: null, info: null, msgs: [
        [false, 'Ê bà, cuối tuần này đi ngày hội nhận nuôi ở công viên không?', 30],
        [true, 'Đi chứ, tui đang tính nhận thêm một bé chó ở Mái Ấm Sài Gòn nè.', 29],
        [false, 'Bé nào vậy, gửi tui coi với.', 28],
        [true, 'Bé Ki, hiền lắm, tui vừa nộp đơn xong.', 27],
        [false, 'Dễ thương ghê! Có gì kể tui nghe nha.', 2],
    ] },
    { n: 2, peer: ['owner', 1, 'Trần Thị Hồng Nhung'], type: 'ADOPTION', related: did('application', 9001),
      name: 'Đơn nhận nuôi A-D9001', info: 'Ki', msgs: [
        [false, 'Chào bạn, trạm đã nhận đơn nhận nuôi bé Ki. Bạn rảnh qua trạm gặp bé vào thứ Bảy được không?', 20],
        [true, 'Dạ được ạ, khoảng 9 giờ sáng em qua nha chị.', 19],
        [false, 'Ok bạn, nhớ mang theo CCCD nhé. Hẹn gặp bạn!', 18],
    ] },
    { n: 3, peer: ['user', 12, USER_NAMES[11]], type: 'RESCUE', related: did('case', 30),
      name: 'Ca R-D0030', info: 'Mèo', msgs: [
        [false, 'Mình nhận ca bạn báo rồi nha, đang chạy qua.', 1],
        [true, 'Cảm ơn bạn nhiều! Bé trốn dưới gầm chiếc xe màu xám đó.', 0.9],
        [false, 'Bế được bé rồi, mình gửi báo cáo hoàn tất, bạn xác nhận giúp mình nhé.', 0.3],
    ] },
];
const chatIds = CHATS.map((c) => did('conversation', c.n));

// UUID as the plain lowercase string the API uses for map keys and the direct key.
const hex = (u) => u.toString('hex').replace(/^(.{8})(.{4})(.{4})(.{4})(.{12})$/, '$1-$2-$3-$4-$5');

function seedChat(c) {
    const id = did('conversation', c.n);
    const me = did('user', 1);
    const peer = did(c.peer[0], c.peer[1]);
    const messages = c.msgs.map(([mine, content, h], i) => ({
        messageId: did('message', c.n * 100 + i + 1), seq: i + 1, senderId: mine ? me : peer, content: content,
        sentAt: hoursAgo(h), createdAt: hoursAgo(h), updatedAt: hoursAgo(h), deleted: false,
    }));
    const last = messages[messages.length - 1];
    const lastMessage = { messageId: last.messageId, seq: last.seq, senderId: last.senderId, preview: last.content, at: last.sentAt };
    const started = messages[0].sentAt;
    const [a, b] = [hex(me), hex(peer)].sort();
    const unreadForMe = hex(last.senderId) === hex(peer) ? 1 : 0;
    const readSeq = {};
    readSeq[hex(me)] = last.seq - unreadForMe;
    readSeq[hex(peer)] = last.seq;

    const fresh = target.conversations.updateOne({ _id: id }, { $setOnInsert: {
        type: c.type, name: c.name, relatedEntityId: c.related, relatedInfo: c.info, participantIds: [me, peer],
        directKey: c.type + ':' + (c.related ? hex(c.related) : '-') + ':' + a + ':' + b,
        seq: last.seq, readSeq: readSeq, lastMessage: lastMessage, lastMessageAt: last.sentAt,
        createdAt: started, createdBy: me, updatedAt: last.sentAt, deleted: false,
    } }, { upsert: true }).upsertedCount;
    if (!fresh) return 0;
    target.message_buckets.updateOne({ _id: hex(id) + ':0' },
        { $setOnInsert: { conversationId: id, bucketNo: NumberLong('0'), messages: messages } }, { upsert: true });
    [[me, peer, c.peer[2], unreadForMe], [peer, me, USER1.fullName, 0]].forEach(([owner, other, otherName, unread]) => {
        target.inboxes.updateOne({ _id: hex(owner) + ':' + hex(id) }, { $setOnInsert: {
            userId: owner, conversationId: id, type: c.type, name: c.name, relatedEntityId: c.related, relatedInfo: c.info,
            peer: { userId: other, name: otherName, avatarUrl: null }, lastMessage: lastMessage, unread: unread,
            sortAt: last.sentAt, joinedAt: started, lastReadAt: unread ? messages[messages.length - 2].sentAt : last.sentAt,
            hidden: false, createdAt: started,
        } }, { upsert: true });
    });
    return 1;
}

const postIds = POSTS.map((_, i) => did('post', i + 1)).concat(OWN_POSTS.map((p) => did('post', p.n)));

if (typeof CLEANUP !== 'undefined' && CLEANUP) {
    print('Removed chats: ' + target.conversations.deleteMany({ _id: { $in: chatIds } }).deletedCount);
    target.message_buckets.deleteMany({ conversationId: { $in: chatIds } });
    target.inboxes.deleteMany({ conversationId: { $in: chatIds } });
    print('Removed posts: ' + target.posts.deleteMany({ _id: { $in: postIds } }).deletedCount);
    print('Removed comments: ' + target.comments.deleteMany({ postId: { $in: postIds } }).deletedCount);
    print('Removed likes: ' + target.post_likes.deleteMany({ '_id.postId': { $in: postIds } }).deletedCount);
} else {
    let written = 0;
    POSTS.forEach((post, i) => {
        const n = i + 1;
        const linked = post.caseN ? rescueCase(post.caseN) : null;
        const authorN = 1 + ((n * 7) % 30);
        // Newest first, a few hours apart, so the feed reads like a busy week.
        const createdAt = new Date(Date.now() - (n * 7 + (n % 3) * 2) * 3600 * 1000);
        const doc = {
            // No username in the snapshot: people sign in with it, so it stays in the users table.
            author: {
                userId: did('user', authorN),
                fullName: USER_NAMES[authorN - 1],
            },
            content: post.text,
            media: [linked ? linked.media : media(post.pet[0], post.pet[1])],
            tags: post.tags,
            stats: { likes: 3 + (n * 11) % 38, comments: 0 },
            previewComments: [],
            facets: {
                category: linked ? 'RESCUE' : post.category,
                species: linked ? linked.species : post.species,
                province: linked ? linked.province : null,
                needsRescue: !!linked && (linked.status === 'REPORTED' || linked.status === 'IN_PROGRESS'),
            },
            createdAt: createdAt,
            updatedAt: createdAt,
            deleted: false,
        };
        if (linked) doc.rescueCase = { caseId: linked.caseId, status: linked.status };
        written += target.posts.updateOne({ _id: did('post', n) }, { $setOnInsert: doc }, { upsert: true }).upsertedCount;
    });
    OWN_POSTS.forEach((p) => {
        const at = hoursAgo(p.hours);
        const doc = {
            author: USER1, content: p.text, media: [media(p.photo[0], p.photo[1])], tags: p.tags,
            stats: { likes: 4, comments: 0 }, previewComments: [], facets: p.facets,
            analysis: {
                status: p.status, schemaVersion: 1, model: 'openrouter/google/gemini-2.5-flash-lite', analyzedAt: at,
                attempts: 1, totalAttempts: 1, suggested: p.suggested, updatedAt: at,
                confirmedBy: p.status === 'CONFIRMED' ? USER1.userId : null,
                confirmedAt: p.status === 'CONFIRMED' ? at : null,
            },
            createdAt: at, createdBy: USER1.userId, updatedAt: at, deleted: false,
        };
        written += target.posts.updateOne({ _id: did('post', p.n) }, { $setOnInsert: doc }, { upsert: true }).upsertedCount;
    });
    const chats = CHATS.reduce((sum, c) => sum + seedChat(c), 0);
    print('Database: ' + target.getName() + '. New posts: ' + written + ', total demo posts: '
        + target.posts.countDocuments({ _id: { $in: postIds } }) + '. New chats: ' + chats);
}
